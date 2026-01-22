package mb.fw.atb.configuration;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indigo.indigomq.pool.PooledConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.sub.RemoteNetworkAdaptor;
import mb.fw.atb.enums.ActionType;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.listener.DataListener;
import mb.fw.atb.listener.ResendListener;
import mb.fw.atb.listener.ResultListener;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.service.NAOutboundSendAsyncService;
import mb.fw.atb.service.NAInboundToHttpService;
import mb.fw.net.common.NetworkAdaptorCallback;
import mb.fw.transformation.form.MessageFormBox;
import mb.fw.transformation.service.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.remoting.JmsInvokerServiceExporter;

import javax.jms.ConnectionFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * IndigoMQ(메시지 큐) Config
 */
@Configuration
@Slf4j
public class MessageQueueConfig {

    @Autowired(required = false)
    PooledConnectionFactory jmsConnectionFactory;
    DefaultJmsListenerContainerFactory factory;
    @Autowired(required = false)
    private JmsListenerEndpointRegistry registry;

    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired(required = false)
        List<ConnectionFactory> eachConnectionFactoryList;

    @Autowired
    private IFConfig ifConfig;

    @Autowired
    private ApplicationContext appContext;

    @Autowired(required = false)
    NAOutboundSendAsyncService asyncCallService;

    @Bean
    public JmsListenerContainerFactory<?> queueListenerFactory() {
        log.info("queueListenerFactory init ==> {}", jmsConnectionFactory);

        if (jmsConnectionFactory == null) {
            return null;
        }

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionTransacted(true);
        factory.setConnectionFactory(jmsConnectionFactory);
        this.factory = factory;
        return factory;
    }

    @DependsOn("autoIfCfg")
    @Bean
    public String registMessageListenerContainer(@Autowired DataListener dataListener, @Autowired ResultListener resultListener, @Autowired ResendListener resendListener) {
        log.info("registMessageListenerContainer init ==> {}", jmsConnectionFactory);

        if (jmsConnectionFactory == null) {
            return "";
        }

        log.info("registMessageListenerContainer eachConnectionFactoryList ==> {}", eachConnectionFactoryList);
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) appContext).getBeanFactory();

        if (eachConnectionFactoryList == null) {
            log.error("eachConnectionFactoryList is null");
            listenProcess(dataListener, resultListener, resendListener, factory, 1);
            return "";
        }

        int i = 1;

        for (ConnectionFactory connectionFactory : eachConnectionFactoryList) {
            DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            factory.setSessionTransacted(true);
            beanFactory.registerSingleton("jmsListenerContainerFactory_" + i++, factory);
            listenProcess(dataListener, resultListener, resendListener, factory, i);
        }

        return "";
    }

    private void listenProcess(DataListener dataListener, ResultListener resultListener, ResendListener resendListener, DefaultJmsListenerContainerFactory factory, int seq) {
        if (ifConfig.getContext() != null && ifConfig.getContext().size() > 0){
            String id = ifConfig.getId();
            StringBuilder interfaceIdListIn = new StringBuilder();

            AtomicBoolean resultListenerBool = new AtomicBoolean(false);
            AtomicBoolean dataListenerBool = new AtomicBoolean(false);

            interfaceIdListIn.append("(");
            for (IFContext ifContext : ifConfig.getContext()) {
                String interfaceId = ifContext.getInterfaceId();
                ActionType actionType = ifContext.getActionType();

                if (actionType == ActionType.SENDER) {
                    resultListenerBool.set(true);
                } else if (actionType == ActionType.RECEIVER) {
                    dataListenerBool.set(true);
                }

                if (interfaceIdListIn.indexOf("'" + interfaceId + "'") > -1) {
                    continue;
                }

                interfaceIdListIn.append("'").append(interfaceId).append("',");
            }

            int commaIdx = interfaceIdListIn.lastIndexOf(",");
            interfaceIdListIn.deleteCharAt(commaIdx).append(")");

            if (resultListenerBool.get()) {
                SimpleJmsListenerEndpoint sEndpoint = new SimpleJmsListenerEndpoint();
                sEndpoint.setId(ifConfig.getPrefix() + "_RESULT_LISTENER_" + seq);
                sEndpoint.setMessageListener(resultListener);
                sEndpoint.setDestination(ifConfig.getPrefix() + ".RESULT");
                sEndpoint.setSelector(THeader.SENDER_ID.key() + "='" + id + "' and " + THeader.INTERFACE_ID.key() + " in " + interfaceIdListIn);
                sEndpoint.setConcurrency(ifConfig.getSenderConcurrency());
                registry.registerListenerContainer(sEndpoint, factory, true);

                SimpleJmsListenerEndpoint resendEndpoint = new SimpleJmsListenerEndpoint();
                resendEndpoint.setId(ifConfig.getPrefix() + "_RESEND_LISTENER_" + seq);
                resendEndpoint.setMessageListener(resendListener);
                resendEndpoint.setDestination("RESEND.Q");

                //구버전 AdaptorTemplate용으로 개발되어져있어서 맞춤
                resendEndpoint.setSelector("IF_ID in " + interfaceIdListIn);
                resendEndpoint.setConcurrency("1");
                registry.registerListenerContainer(resendEndpoint, factory, true);
            }

            if (dataListenerBool.get()) {
                SimpleJmsListenerEndpoint tEndpoint = new SimpleJmsListenerEndpoint();
                tEndpoint.setId(ifConfig.getPrefix() + "_DATA_LISTENER_" + seq);
                tEndpoint.setMessageListener(dataListener);
                tEndpoint.setDestination(ifConfig.getPrefix() + ".DATA");
                tEndpoint.setSelector(THeader.RECEIVER_ID.key() + "='" + id + "' and " + THeader.INTERFACE_ID.key() + " in " + interfaceIdListIn);
                tEndpoint.setConcurrency(ifConfig.getReceiverConcurrency());
                registry.registerListenerContainer(tEndpoint, factory, true);
            }
        }
    }

    HashMap<String, MessageFormBox> myMessageFormBox = Maps.newHashMap();

    @Bean(name = "myMessageFormBox")
    @DependsOn("registNetworkAdaptorAndBeans")
    public HashMap<String, MessageFormBox> myMessageFormBox(@Autowired(required = false) @Qualifier("mapperService") MapperService mapperService) {
        log.info("myMessageFormBox init");

        log.info("mapperService: {}", mapperService);
        if (mapperService == null) {
            return myMessageFormBox;
        }
        List<IFContext> context = ifConfig.getContext();

        HashSet<String> groupIds = Sets.newHashSet();
        ifConfig.getContext().stream().filter(ifContext -> ifContext.getMapperGroupId() != null).map(IFContext::getMapperGroupId).forEach(groupIds::add);

        groupIds.forEach(groupId -> {
            log.info("REMOTE REQUEST MAPPER GROUPID... : {}", groupId);
            MessageFormBox messageFormBox = mapperService.getMessageFormBox(groupId);
            log.info("REMOTE REQUEST MAPPER GROUPID...OK : {}", groupId);
            myMessageFormBox.put(groupId, messageFormBox);
        });

        log.info("generate myMessageFormBox: {}", myMessageFormBox);
        return myMessageFormBox;
    }


    @Bean(name = "networkAdaptorJMSInboundService")
    @DependsOn("jmsTemplate")
    public NAInboundToHttpService networkAdaptorJMSInboundService() {
        log.info("networkAdaptorJMSInboundService init");
        NAInboundToHttpService networkAdaptorInbound = new NAInboundToHttpService();
        networkAdaptorInbound.setIfConfig(ifConfig);
        networkAdaptorInbound.setMyMessageFormBox(myMessageFormBox);
        networkAdaptorInbound.setJmsTemplate(jmsTemplate);
        networkAdaptorInbound.setAppContext(appContext);
        networkAdaptorInbound.setNaOutboundSendAsyncService(asyncCallService);
        return networkAdaptorInbound;
    }

    @Bean
    @DependsOn("networkAdaptorJMSInboundService")
    public SimpleMessageListenerContainer networkCallListenerContainer(@Autowired NAInboundToHttpService providerInboundNAService) {
        log.info("networkCallListenerContainer init");
        /**
         * NetworkAdpator call Configuration
         */
        RemoteNetworkAdaptor remoteNetworkAdaptorCfg = ifConfig.getRemoteNetworkAdaptor();

        if (remoteNetworkAdaptorCfg == null) {
            return null;
        }

        String naCallQueue = remoteNetworkAdaptorCfg.getInboundQueue();


        log.info("networkAdaptorInbound : {}", providerInboundNAService);
        //콜서비스 리스너
        if (naCallQueue != null) {
            JmsInvokerServiceExporter exporter = new JmsInvokerServiceExporter();
            exporter.setServiceInterface(NetworkAdaptorCallback.class);
            exporter.setService(providerInboundNAService);
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            container.setConnectionFactory(jmsConnectionFactory);
            container.setDestinationName(naCallQueue);
            container.setMessageListener(exporter);
            container.setConcurrentConsumers(10);
            exporter.afterPropertiesSet();
            log.info("networkAdaptorInbound Queue : {}", naCallQueue);
            return container;
        }

        return null;
    }
}