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
package org.apache.camel.component.xmlsecurity.api;

/**
 * Exception thrown when a configuration failure or a failure caused by the
 * input message in the XML signature generation or validation process occurs.
 * 
 * The route developer can catch these exception in an error handler to react on
 * such failures.
 */
public class XmlSignatureException extends Exception {

    private static final long serialVersionUID = 1L;

    public XmlSignatureException() {
    }

    public XmlSignatureException(String message) {
        super(message);
    }

    public XmlSignatureException(Throwable cause) {
        super(cause);
    }

    public XmlSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

}
