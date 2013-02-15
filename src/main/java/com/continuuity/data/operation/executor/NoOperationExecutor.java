package com.continuuity.data.operation.executor;

import com.continuuity.api.data.OperationException;
import com.continuuity.api.data.OperationResult;
import com.continuuity.data.operation.ClearFabric;
import com.continuuity.data.operation.OpenTable;
import com.continuuity.data.operation.OperationContext;
import com.continuuity.data.operation.Read;
import com.continuuity.data.operation.ReadAllKeys;
import com.continuuity.data.operation.ReadColumnRange;
import com.continuuity.data.operation.ReadKey;
import com.continuuity.data.operation.StatusCode;
import com.continuuity.data.operation.WriteOperation;
import com.continuuity.data.operation.ttqueue.DequeueResult;
import com.continuuity.data.operation.ttqueue.QueueAdmin;
import com.continuuity.data.operation.ttqueue.QueueDequeue;

import java.util.List;
import java.util.Map;

import static com.continuuity.data.operation.ttqueue.QueueAdmin.QueueInfo;

/**
 * This is an implementation of OperationExecutor that does nothing but
 * return empty results. It is useful for testing and performance benchmarks.
 */
public class NoOperationExecutor implements OperationExecutor {

  @Override
  public String getName() {
    return "noop";
  }

  @Override
  public void execute(OperationContext context,
                      List<WriteOperation> writes) throws OperationException {
    // do nothing
  }

  @Override
  public Transaction startTransaction(OperationContext context) throws OperationException {
    return null;
  }

  @Override
  public Transaction submit(OperationContext context,
                            Transaction transaction,
                            List<WriteOperation> writes)
    throws OperationException {
    execute(context, writes);
    return null;
  }

  @Override
  public void commit(OperationContext context,
                     Transaction transaction)
    throws OperationException {
    // do nothing
  }

  @Override
  public void abort(OperationContext context,
                    Transaction transaction)
    throws OperationException {
    // do nothing
  }

  @Override
  public DequeueResult execute(OperationContext context,
                               QueueDequeue dequeue) {
    // pretend the queue is empty
    return new DequeueResult(DequeueResult.DequeueStatus.EMPTY);
  }

  @Override
  public long execute(OperationContext context,
                      QueueAdmin.GetGroupID getGroupId) {
    return 0L;
  }

  @Override
  public OperationResult<QueueInfo>
  execute(OperationContext context, QueueAdmin.GetQueueInfo getQueueInfo) {
    // pretend the queue does not exist
    return new OperationResult<QueueInfo>(StatusCode.QUEUE_NOT_FOUND);
  }

  @Override
  public void execute(OperationContext context,
                      ClearFabric clearFabric) {
    // do nothing
  }

  @Override
  public void execute(OperationContext context,
                      OpenTable openTable) throws OperationException {
    // do nothing
  }

  @Override
  public OperationResult<byte[]> execute(OperationContext context,
                                         ReadKey read)
      throws OperationException {
    // return empty result, key not found
    return new OperationResult<byte[]>(StatusCode.KEY_NOT_FOUND);
  }

  @Override
  public OperationResult<Map<byte[], byte[]>>
  execute(OperationContext context, Read read) {
    // return empty result, key not found
    return new OperationResult<Map<byte[], byte[]>>(StatusCode.KEY_NOT_FOUND);
  }

  @Override
  public OperationResult<List<byte[]>> execute(
      OperationContext context, ReadAllKeys readKeys) {
    return new OperationResult<List<byte[]>>(StatusCode.KEY_NOT_FOUND);

  }

  @Override
  public OperationResult<Map<byte[], byte[]>>
  execute(OperationContext context, ReadColumnRange readColumnRange) {
    // pretend the key does not exists
    return new OperationResult<Map<byte[], byte[]>>(StatusCode.KEY_NOT_FOUND);
  }

  @Override
  public void execute(OperationContext context, WriteOperation write)
      throws OperationException {
    // do nothing
  }

}
