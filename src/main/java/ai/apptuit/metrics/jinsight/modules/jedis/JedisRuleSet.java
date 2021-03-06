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

package ai.apptuit.metrics.jinsight.modules.jedis;

import ai.apptuit.metrics.jinsight.modules.common.AbstractRuleSet;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.util.Pool;

/**
 * @author Rajiv Shivane
 */
public class JedisRuleSet extends AbstractRuleSet {

  private static final String HELPER_NAME =
      "ai.apptuit.metrics.jinsight.modules.jedis.JedisRuleHelper";

  private final List<RuleInfo> rules = new ArrayList<>();

  public JedisRuleSet() {
    interceptJedisCommands();
    interceptJedisTransaction();
    interceptJedisPipeline();
    interceptJedisPool();
  }

  private void interceptJedisPool() {
    addRule(Pool.class, "getResource", RuleInfo.AT_ENTRY, "onPoolGetStart($0)");
    addRule(Pool.class, "getResource", RuleInfo.AT_EXIT, "onPoolGetEnd($0)");
    addRule(Pool.class, "getResource", RuleInfo.AT_EXCEPTION_EXIT, "onPoolGetError($0)");
    addRule(Pool.class, "returnResource", RuleInfo.AT_ENTRY, "onPoolReleaseStart($0)");
    addRule(Pool.class, "returnBrokenResource", RuleInfo.AT_ENTRY, "onPoolReleaseStart($0)");
    addRule(Pool.class, "returnResourceObject", RuleInfo.AT_ENTRY, "onPoolReleaseStart($0)");
    addRule(Pool.class, "returnResource", RuleInfo.AT_EXIT, "onPoolReleaseEnd($0)");
    addRule(Pool.class, "returnBrokenResource", RuleInfo.AT_EXIT, "onPoolReleaseEnd($0)");
    addRule(Pool.class, "returnResourceObject", RuleInfo.AT_EXIT, "onPoolReleaseEnd($0)");
    addRule(Pool.class, "returnResource", RuleInfo.AT_EXCEPTION_EXIT, "onPoolReleaseError($0)");
    addRule(Pool.class, "returnBrokenResource", RuleInfo.AT_EXCEPTION_EXIT,
        "onPoolReleaseError($0)");
    addRule(Pool.class, "returnResourceObject", RuleInfo.AT_EXCEPTION_EXIT,
        "onPoolReleaseError($0)");
  }

  private void interceptJedisPipeline() {
    addRule(Pipeline.class, RuleInfo.CONSTRUCTOR_METHOD, RuleInfo.AT_EXIT,
        "onPipelineBegin($0)");
    addRule(Pipeline.class, "sync", RuleInfo.AT_EXIT,
        "onPipelineSync($0)");
  }

  private void interceptJedisTransaction() {
    addRule(Transaction.class, RuleInfo.CONSTRUCTOR_METHOD, RuleInfo.AT_EXIT,
        "onTransactionBegin($0)");
    addRule(Transaction.class, "exec", RuleInfo.AT_EXIT,
        "onTransactionExec($0)");
    addRule(Transaction.class, "execGetResponse", RuleInfo.AT_EXIT,
        "onTransactionExec($0)");
    addRule(Transaction.class, "discard", RuleInfo.AT_EXIT,
        "onTransactionDiscard($0)");
  }

  private void interceptJedisCommands() {
    Class<JedisCommands> clazz = JedisCommands.class;
    Method[] methods = clazz.getDeclaredMethods();
    Set<String> methodNames = new HashSet<>();
    for (Method method : methods) {
      if (methodNames.contains(method.getName())) {
        continue;//over-loaded method
      }
      methodNames.add(method.getName());
      addRulesForOperation(method);
    }
  }

  private void addRulesForOperation(Method method) {
    String methodName = method.getName();
    addRule(Jedis.class, methodName, RuleInfo.AT_ENTRY,
        "onOperationStart(\"" + methodName + "\", $0)");
    addRule(Jedis.class, methodName, RuleInfo.AT_EXIT,
        "onOperationEnd(\"" + methodName + "\", $0)");
    addRule(Jedis.class, methodName, RuleInfo.AT_EXCEPTION_EXIT,
        "onOperationError(\"" + methodName + "\", $0)");
  }

  private void addRule(Class clazz, String methodName, String whereClause, String action) {
    String ruleName = clazz + " " + methodName + " " + whereClause;
    RuleInfo rule = new RuleInfo(ruleName, clazz.getName(), clazz.isInterface(), false,
        methodName, HELPER_NAME, whereClause, null, null, action, null, null);
    rules.add(rule);
  }

  @Override
  public List<RuleInfo> getRules() {
    return rules;
  }
}
