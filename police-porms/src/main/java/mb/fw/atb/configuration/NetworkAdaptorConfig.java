package mb.fw.atb.configuration;

import com.indigo.indigomq.pool.PooledConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.RemoteNetworkAdaptor;
import mb.fw.net.common.NetworkAdaptorAPI;
import mb.fw.transformation.service.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.remoting.JmsInvokerProxyFactoryBean;

import java.util.Map;

/**
 * IndigoMQ(메시지 큐) Config
 */
@EnableJms
@Configuration
@Slf4j
public class NetworkAdaptorConfig {

    @Autowired(required = false)
    PooledConnectionFactory jmsConnectionFactory;

    @Autowired(required = false)
    DefaultJmsListenerContainerFactory factory;

    @Autowired(required = false)
    private JmsListenerEndpointRegistry registry;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    @Autowired
    private IFConfig ifConfig;

    @Autowired
    private ApplicationContext appContext;

    @Bean
    public String registNetworkAdaptorAndBeans() {

        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) appContext).getBeanFactory();
        log.info("beanFactory: {}", beanFactory);
        RemoteNetworkAdaptor remoteNetworkAdaptorCfg = ifConfig.getRemoteNetworkAdaptor();

        if (remoteNetworkAdaptorCfg != null) {
            Map<String, String> naRemoteQueue = remoteNetworkAdaptorCfg.getOutboundQueueMap();

            log.info("naRemoteQueue: {}", naRemoteQueue);
            if (naRemoteQueue != null) {
                for (String naId : naRemoteQueue.keySet()) {
                    JmsInvokerProxyFactoryBean factoryBean = new JmsInvokerProxyFactoryBean();
                    factoryBean.setConnectionFactory(jmsConnectionFactory);
                    factoryBean.setServiceInterface(NetworkAdaptorAPI.class);
                    factoryBean.setQueueName(naRemoteQueue.get(naId));
                    factoryBean.setReceiveTimeout(remoteNetworkAdaptorCfg.getReceiveTimeout());
                    factoryBean.afterPropertiesSet();
                    beanFactory.registerSingleton(naId, factoryBean.getObject());
                    log.info("{} RemoteQueue is registered", naId);
                }
            }
        }

        String mapperRemoteQueue = ifConfig.getMapperRemoteQueue();

        if (mapperRemoteQueue != null) {
            JmsInvokerProxyFactoryBean factoryBean = new JmsInvokerProxyFactoryBean();
            factoryBean.setConnectionFactory(jmsConnectionFactory);
            factoryBean.setServiceInterface(MapperService.class);
            factoryBean.setQueueName(mapperRemoteQueue);
            factoryBean.setReceiveTimeout(ifConfig.getMapperReceiveTimeout());
            factoryBean.afterPropertiesSet();
            beanFactory.registerSingleton("mapperService", factoryBean.getObject());
            log.info("mapperService is registered");
        }

        return "";
    }


}
