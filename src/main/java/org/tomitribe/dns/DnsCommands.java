/*
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
package org.tomitribe.dns;

import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Option;
import org.tomitribe.crest.api.Required;
import org.tomitribe.crest.api.StreamingOutput;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

public class DnsCommands {

    @Command("check-local")
    public StreamingOutput checkLocal() {
        return new StreamingOutput() {
            @Override
            public void write(final OutputStream outputStream) throws IOException {
                final PrintWriter writer = new PrintWriter(outputStream);

                writer.println("Looking up localhost...\n");
                writer.flush();

                final long start = System.currentTimeMillis();
                final InetAddress localHost = InetAddress.getLocalHost();
                final long end = System.currentTimeMillis();

                writer.println(String.format("Found in %d ms: %s", (end - start), localHost.toString()));
                writer.flush();
            }
        };
    }

    @Command("check-java")
    public StreamingOutput checkJava(final @Required @Option("hostname") String hostName) {
        return new StreamingOutput() {
            @Override
            public void write(final OutputStream outputStream) throws IOException {
                final PrintWriter writer = new PrintWriter(outputStream);

                writer.println(String.format("Looking up %s...\n", hostName));
                writer.flush();

                final long start = System.currentTimeMillis();
                final InetAddress address = InetAddress.getByName(hostName);
                final long end = System.currentTimeMillis();

                writer.println(String.format("Found in %d ms: %s", (end - start), address.toString()));
                writer.flush();
            }
        };
    }


    @Command("check-jndi")
    public StreamingOutput check(final @Required @Option("hostname") String hostName, final @Required @Option("type") String type) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                final PrintWriter writer = new PrintWriter(outputStream);

                writer.println(String.format("Looking up %s record for %s...\n", type, hostName));
                writer.flush();

                final long start = System.currentTimeMillis();
                final Set<String> results = new TreeSet<>();
                try {
                    final Hashtable<String, String> envProps = new Hashtable<>();
                    envProps.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

                    final DirContext dnsContext = new InitialDirContext(envProps);
                    final Attributes dnsEntries = dnsContext.getAttributes(hostName, new String[]{type});

                    if (dnsEntries != null) {
                        final NamingEnumeration<?> dnsEntryIterator = dnsEntries.get(type).getAll();
                        while(dnsEntryIterator.hasMoreElements()) {
                            results.add(dnsEntryIterator.next().toString());
                        }
                    }
                } catch (Exception e) {
                    writer.println("Error while looking up record: " + e.getMessage());
                    e.printStackTrace(writer);
                    writer.flush();
                }

                final long end = System.currentTimeMillis();
                writer.println(String.format("Lookup completed in %d ms", (end - start)));
                writer.flush();

                writer.println(String.format("Found %d records", results.size()));

                for (final String result : results) {
                    writer.println(String.format("Result: %s", result));
                }

                writer.flush();
            }
        };
    }

}
