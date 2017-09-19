/*
 * Copyright 2017 Agilx, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.apptuit.metrics.bci;

import ai.apptuit.metrics.dropwizard.TagEncodedMetricName;
import com.codahale.metrics.Gauge;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.extension.CacheExtension;
import org.jboss.byteman.rule.Rule;

/**
 * @author <a href="mailto:rajiv.shivane@apptuit.ai">Rajiv</a>
 * @since 9/6/2017
 */
public class EhcacheRuleHelper extends RuleHelper {

  public static final TagEncodedMetricName ROOT_NAME = TagEncodedMetricName.decode("ehcache");

  public EhcacheRuleHelper(Rule rule) {
    super(rule);
  }

  public void monitor(Cache cache) {
    cache.registerCacheExtension(new CacheLifecycleListener(cache));

  }

  private static class CacheLifecycleListener implements CacheExtension {

    private final Cache cache;
    private Status status;

    public CacheLifecycleListener(Cache cache) {
      this.cache = cache;
      status = Status.STATUS_UNINITIALISED;
    }

    @Override
    public void init() {
      register("hits", () -> cache.getStatistics().cacheHitCount());
      register("in_memory_hits", () -> cache.getStatistics().localHeapHitCount());
      register("off_heap_hits", () -> cache.getStatistics().localOffHeapHitCount());
      register("on_disk_hits", () -> cache.getStatistics().localDiskHitCount());
      register("misses", () -> cache.getStatistics().cacheMissCount());
      register("in_memory_misses", () -> cache.getStatistics().localHeapMissCount());
      register("off_heap_misses", () -> cache.getStatistics().localOffHeapMissCount());
      register("on_disk_misses", () -> cache.getStatistics().localDiskMissCount());
      register("objects", () -> cache.getStatistics().getSize());
      register("in_memory_objects", () -> cache.getStatistics().getLocalHeapSize());
      register("off_heap_objects", () -> cache.getStatistics().getLocalOffHeapSize());
      register("on_disk_objects", () -> cache.getStatistics().getLocalDiskSize());
      register("mean_get_time",
          () -> cache.getStatistics().cacheGetOperation().latency().average().value());
      register("mean_search_time",
          () -> cache.getStatistics().cacheSearchOperation().latency().average().value());
      register("eviction_count",
          () -> cache.getStatistics().cacheEvictionOperation().count().value());
      register("searches_per_second",
          () -> cache.getStatistics().cacheSearchOperation().rate().value());
      register("writer_queue_size", () -> cache.getStatistics().getWriterQueueLength());

      status = Status.STATUS_ALIVE;
    }

    private void register(String metric, Gauge<Number> gauge) {
      registry
          .register(ROOT_NAME.submetric(metric, "cache", cache.getName()).toString(), gauge);
    }

    @Override
    public void dispose() throws CacheException {
      //new Exception("Disposing cache: [" + cache.getName() + "]").printStackTrace();
      //TODO unregister the metrics?!
      status = Status.STATUS_SHUTDOWN;
    }

    @Override
    public CacheExtension clone(Ehcache cache) throws CloneNotSupportedException {
      throw new CloneNotSupportedException();
    }

    @Override
    public Status getStatus() {
      Thread.dumpStack();
      return status;
    }
  }
}
