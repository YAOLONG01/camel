/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.processor.idempotent;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.impl.ServiceSupport;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.util.ServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An implementation of the <a
 * href="http://camel.apache.org/idempotent-consumer.html">Idempotent
 * Consumer</a> pattern.
 * 
 * @version $Revision$
 */
public class IdempotentConsumer extends ServiceSupport implements Processor {
    private static final transient Log LOG = LogFactory.getLog(IdempotentConsumer.class);
    private Expression messageIdExpression;
    private Processor nextProcessor;
    private IdempotentRepository idempotentRepository;

    public IdempotentConsumer(Expression messageIdExpression, 
            IdempotentRepository idempotentRepository, Processor nextProcessor) {
        this.messageIdExpression = messageIdExpression;
        this.idempotentRepository = idempotentRepository;
        this.nextProcessor = nextProcessor;
    }

    @Override
    public String toString() {
        return "IdempotentConsumer[expression=" + messageIdExpression + ", repository=" + idempotentRepository
               + ", processor=" + nextProcessor + "]";
    }

    public void process(Exchange exchange) throws Exception {
        String messageId = messageIdExpression.evaluate(exchange, String.class);
        if (messageId == null) {
            throw new NoMessageIdException(exchange, messageIdExpression);
        }
        if (idempotentRepository.add(messageId)) {
            nextProcessor.process(exchange);
        } else {
            onDuplicateMessage(exchange, messageId);
        }
    }

    // Properties
    // -------------------------------------------------------------------------
    public Expression getMessageIdExpression() {
        return messageIdExpression;
    }

    public IdempotentRepository getIdempotentRepository() {
        return idempotentRepository;
    }

    public Processor getNextProcessor() {
        return nextProcessor;
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    protected void doStart() throws Exception {
        ServiceHelper.startServices(nextProcessor);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopServices(nextProcessor);
    }

    /**
     * A strategy method to allow derived classes to overload the behaviour of
     * processing a duplicate message
     * 
     * @param exchange the exchange
     * @param messageId the message ID of this exchange
     */
    protected void onDuplicateMessage(Exchange exchange, String messageId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Ignoring duplicate message with id: " + messageId + " for exchange: " + exchange);
        }
    }
}
