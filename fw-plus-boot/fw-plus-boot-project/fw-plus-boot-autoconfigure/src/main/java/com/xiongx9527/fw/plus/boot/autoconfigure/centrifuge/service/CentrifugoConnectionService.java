package com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config.CentrifugoInstanceProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.config.CentrifugoProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.listener.CentrifugoMessageListener;
import io.github.centrifugal.centrifuge.Client;
import io.github.centrifugal.centrifuge.CompletionCallback;
import io.github.centrifugal.centrifuge.ConnectionTokenGetter;
import io.github.centrifugal.centrifuge.ErrorEvent;
import io.github.centrifugal.centrifuge.EventListener;
import io.github.centrifugal.centrifuge.JoinEvent;
import io.github.centrifugal.centrifuge.MessageEvent;
import io.github.centrifugal.centrifuge.Options;
import io.github.centrifugal.centrifuge.PublishResult;
import io.github.centrifugal.centrifuge.ResultCallback;
import io.github.centrifugal.centrifuge.ServerJoinEvent;
import io.github.centrifugal.centrifuge.ServerLeaveEvent;
import io.github.centrifugal.centrifuge.ServerPublicationEvent;
import io.github.centrifugal.centrifuge.ServerSubscribedEvent;
import io.github.centrifugal.centrifuge.ServerSubscribingEvent;
import io.github.centrifugal.centrifuge.ServerUnsubscribedEvent;
import io.github.centrifugal.centrifuge.SubscribingEvent;
import io.github.centrifugal.centrifuge.ConnectedEvent;
import io.github.centrifugal.centrifuge.DisconnectedEvent;
import io.github.centrifugal.centrifuge.Subscription;
import io.github.centrifugal.centrifuge.SubscriptionEventListener;
import io.github.centrifugal.centrifuge.SubscribedEvent;
import io.github.centrifugal.centrifuge.LeaveEvent;
import io.github.centrifugal.centrifuge.PublicationEvent;
import io.github.centrifugal.centrifuge.SubscriptionErrorEvent;
import io.github.centrifugal.centrifuge.UnsubscribedEvent;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CentrifugoConnectionService {

    private final CentrifugoProperties properties;
    private final List<CentrifugoMessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final Map<String, Client> clients = new ConcurrentHashMap<>();
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private ConnectionTokenGetter tokenGetter;

    public CentrifugoConnectionService(CentrifugoProperties properties, ConnectionTokenGetter tokenGetter) {
        this.properties = properties;
        this.tokenGetter = tokenGetter;
    }

    public void addMessageListener(CentrifugoMessageListener listener) {
        if (listener != null) {
            messageListeners.add(listener);
        }
    }

    @PostConstruct
    public void start() {
        if (!properties.isEnabled()) {
            return;
        }

        List<CentrifugoInstanceProperties> instances = resolveInstances();
        if (instances.isEmpty()) {
            return;
        }

        for (CentrifugoInstanceProperties instance : instances) {
            if (!instance.isEnabled()) {
                continue;
            }

            EventListener eventListener = new EventListener() {
                @Override
                public void onConnected(Client client, ConnectedEvent event) {
                    String clientId = instance.getName() + ":" + event.getClient();
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onConnected(clientId);
                    }
                }

                @Override
                public void onDisconnected(Client client, DisconnectedEvent event) {
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onDisconnected(event.getCode(), event.getReason());
                    }
                }

                @Override
                public void onError(Client client, ErrorEvent event) {
                    failureCount.incrementAndGet();
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onError(event.getError());
                    }
                }

                @Override
                public void onMessage(Client client, MessageEvent event) {
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onMessage(null, event.getData());
                    }
                }

                @Override
                public void onSubscribed(Client client, ServerSubscribedEvent event) {
                    String clientId = instance.getName() + ":" + event.getChannel();
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onConnected(clientId);
                    }
                }

                @Override
                public void onSubscribing(Client client, ServerSubscribingEvent event) {
                }

                @Override
                public void onUnsubscribed(Client client, ServerUnsubscribedEvent event) {
                    String channel = event.getChannel();
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onDisconnected(0, channel);
                    }
                }

                @Override
                public void onPublication(Client client, ServerPublicationEvent event) {
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onMessage(event.getChannel(), event.getData());
                    }
                }

                @Override
                public void onJoin(Client client, ServerJoinEvent event) {
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onJoin(event.getChannel(), event.getInfo());
                    }
                }

                @Override
                public void onLeave(Client client, ServerLeaveEvent event) {
                    for (CentrifugoMessageListener listener : messageListeners) {
                        listener.onLeave(event.getChannel(), event.getInfo());
                    }
                }
            };

            Options options = new Options();
            options.setName(instance.getClientName() != null ? instance.getClientName() : instance.getName());
            if (tokenGetter != null) {
                options.setTokenGetter(tokenGetter);
            }
            if (instance.getToken() != null) {
                options.setToken(instance.getToken());
            }

            Client client = new Client(instance.getEndpoint(), options, eventListener);
            client.connect();
            clients.put(instance.getName(), client);
        }
    }

    public void publish(String instanceName, String channel, byte[] data) {
        Client client = clients.get(instanceName);
        if (client == null || channel == null) {
            return;
        }
        client.publish(channel, data, new ResultCallback<PublishResult>() {
            @Override
            public void onDone(Throwable err, PublishResult res) {
                if (err != null) {
                    failureCount.incrementAndGet();
                } else {
                    successCount.incrementAndGet();
                }
            }
        });
    }

    public void publishToAll(String channel, byte[] data) {
        for (Map.Entry<String, Client> entry : clients.entrySet()) {
            Client client = entry.getValue();
            if (client == null || channel == null) {
                continue;
            }
            client.publish(channel, data, new ResultCallback<PublishResult>() {
                @Override
                public void onDone(Throwable err, PublishResult res) {
                    if (err != null) {
                        failureCount.incrementAndGet();
                    } else {
                        successCount.incrementAndGet();
                    }
                }
            });
        }
    }

    public void send(String instanceName, String channel, byte[] data) {
        Client client = clients.get(instanceName);
        if (client == null || channel == null) {
            return;
        }
        client.send(data, new CompletionCallback() {
            @Override
            public void onDone(Throwable err) {
                if (err != null) {
                    failureCount.incrementAndGet();
                } else {
                    successCount.incrementAndGet();
                }
            }
        });
    }

    public void subscribe(String instanceName, String channel) {
        subscribe(instanceName, channel, null);
    }

    public void subscribe(String instanceName, String channel, SubscriptionEventListener listener) {
        Client client = clients.get(instanceName);
        if (client == null || channel == null) {
            return;
        }

        SubscriptionEventListener subscriptionEventListener = new SubscriptionEventListener() {
            @Override
            public void onPublication(Subscription subscription, PublicationEvent event) {
                if (listener != null) {
                    listener.onPublication(subscription, event);
                }
                for (CentrifugoMessageListener messageListener : messageListeners) {
                    messageListener.onMessage(channel, event.getData());
                }
            }

            @Override
            public void onSubscribed(Subscription subscription, SubscribedEvent event) {
                if (listener != null) {
                    listener.onSubscribed(subscription, event);
                }
            }

            @Override
            public void onSubscribing(Subscription subscription, SubscribingEvent event) {
                if (listener != null) {
                    listener.onSubscribing(subscription, event);
                }
            }

            @Override
            public void onUnsubscribed(Subscription subscription, UnsubscribedEvent event) {
                if (listener != null) {
                    listener.onUnsubscribed(subscription, event);
                }
            }

            @Override
            public void onJoin(Subscription subscription, JoinEvent event) {
                if (listener != null) {
                    listener.onJoin(subscription, event);
                }
            }

            @Override
            public void onLeave(Subscription subscription, LeaveEvent event) {
                if (listener != null) {
                    listener.onLeave(subscription, event);
                }
            }

            @Override
            public void onError(Subscription subscription, SubscriptionErrorEvent event) {
                failureCount.incrementAndGet();
                if (listener != null) {
                    listener.onError(subscription, event);
                }
            }
        };

        Subscription subscription;
        try {
            subscription = client.newSubscription(channel, subscriptionEventListener);
        } catch (Exception e) {
            failureCount.incrementAndGet();
            return;
        }
        subscription.subscribe();
    }

    public void unsubscribe(String instanceName, String channel) {
        Client client = clients.get(instanceName);
        if (client == null || channel == null) {
            return;
        }
        Subscription subscription = client.getSubscription(channel);
        if (subscription != null) {
            subscription.unsubscribe();
            client.removeSubscription(subscription);
        }
    }

    public List<String> getInstanceNames() {
        List<String> names = new ArrayList<>(clients.keySet());
        names.sort(String::compareTo);
        return names;
    }

    private List<CentrifugoInstanceProperties> resolveInstances() {
        List<CentrifugoInstanceProperties> instances = properties.getInstances();
        if (instances != null && !instances.isEmpty()) {
            return instances;
        }

        CentrifugoInstanceProperties single = new CentrifugoInstanceProperties();
        single.setName(properties.getSingle().getName());
        single.setEndpoint(properties.getSingle().getEndpoint());
        single.setToken(properties.getSingle().getToken());
        single.setEnabled(true);

        return Collections.singletonList(single);
    }

    @PreDestroy
    public void stop() {
        for (Client client : clients.values()) {
            if (client != null) {
                client.disconnect();
                try {
                    client.close(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        clients.clear();
    }
}
