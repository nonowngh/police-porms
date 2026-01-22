/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.crsh.plugin.Embedded;
import org.crsh.plugin.PluginDiscovery;
import org.crsh.util.Utils;
import org.crsh.vfs.spi.FSMountFactory;
import org.crsh.vfs.spi.file.FileMountFactory;
import org.crsh.vfs.spi.url.ClassPathMountFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Slf4j
public class SpringBootstrap extends Embedded implements
        BeanClassLoaderAware,
        BeanFactoryAware,
        InitializingBean,
        DisposableBean {


    ObjectMapper mapper = new ObjectMapper();
    /**
     * .
     */
    private ClassLoader loader;

    /**
     * .
     */
    private BeanFactory factory;

    /**
     * .
     */
    protected final HashMap<String, FSMountFactory<?>> drivers = new HashMap<String, FSMountFactory<?>>();

    /**
     * .
     */
    private String cmdMountPointConfig;

    /**
     * .
     */
    private String confMountPointConfig;

    public SpringBootstrap() {
    }

    public String getCmdMountPointConfig() {
        return cmdMountPointConfig;
    }

    public void setCmdMountPointConfig(String cmdMountPointConfig) {
        this.cmdMountPointConfig = cmdMountPointConfig;
    }

    public String getConfMountPointConfig() {
        return confMountPointConfig;
    }

    public void setConfMountPointConfig(String confMountPointConfig) {
        this.confMountPointConfig = confMountPointConfig;
    }

    public void setBeanClassLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public void setBeanFactory(BeanFactory factory) throws BeansException {
        this.factory = factory;
    }

    public void afterPropertiesSet() throws Exception {

        createCommandsDir();
        // Initialise the registerable drivers
        try {
            drivers.put("classpath", new ClassPathMountFactory(loader));
            log.info("getCurrentDirectory : " + Utils.getCurrentDirectory());
            drivers.put("file", new FileMountFactory(Utils.getCurrentDirectory()));
        } catch (Exception e) {
            log.error("Coult not initialize classpath driver", e);
            return;
        }

        // List beans
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("factory", factory);
        if (factory instanceof ListableBeanFactory) {
            ListableBeanFactory listable = (ListableBeanFactory) factory;
            attributes.put("beans", new SpringMap(listable));
        }

        //
        PluginDiscovery discovery = new SpringPluginDiscovery(loader, factory);

        //log.info("attributes : ", attributes.toString());
        log.info("loader : {}", loader.toString());

        start(Collections.unmodifiableMap(attributes), discovery, loader);
    }

    private void createCommandsDir() throws IOException {
        String commandsDir = System.getProperty("user.dir") + "/commands";
        Path commandsPath = Paths.get(commandsDir);

        if (!Files.exists(commandsPath)) {
            commandsPath = Files.createDirectory(commandsPath);
            log.info("commands directory created : " + commandsPath.toString());

            ClassPathResource classPathResource = new ClassPathResource("commands.json");
            String json = IOUtils.toString(classPathResource.getURI());
            List<String> classpathCommands = mapper.readValue(json, List.class);

            classpathCommands.forEach(command -> {
                ClassPathResource subClasspathResource = new ClassPathResource(command);

                Path writePath = Paths.get(commandsDir + "/" + subClasspathResource.getFilename());
                log.info("writePath : " + writePath.toString());

                try {
                    Files.copy(subClasspathResource.getInputStream(), writePath);
                    log.info("copy classpath resource to commands directory : " + writePath.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    protected Map<String, FSMountFactory<?>> getMountFactories() {
        return drivers;
    }

    @Override
    protected String resolveConfMountPointConfig() {
        return confMountPointConfig != null ? confMountPointConfig : getDefaultConfMountPointConfig();
    }

    @Override
    protected String resolveCmdMountPointConfig() {
        return cmdMountPointConfig != null ? cmdMountPointConfig : getDefaultCmdMountPointConfig();
    }

    protected String getDefaultCmdMountPointConfig() {
        return "classpath:/crash/commands/";
    }

    protected String getDefaultConfMountPointConfig() {
        return "classpath:/crash/";
    }

    public void destroy() throws Exception {
        stop();
    }
}
