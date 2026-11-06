/*
 * Copyright (Change Date see Readme) gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */

package de.gematik.erpione.server.jpa.factory;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kaczmarzyk.spring.data.jpa.swagger.springdoc.SpecificationArgResolverSpringdocOperationCustomizer;
import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EntityScan("de.gematik.erpione.server.jpa.entity")
@EnableJpaRepositories("de.gematik.erpione.server.jpa.repo")
@EnableTransactionManagement
@RequiredArgsConstructor
public class ErpioneJpaConfig implements WebMvcConfigurer {

  private final DataSourceProperties ds;

  @Bean
  public DataSource dataSource() {
    val dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName(ds.getDriverClassName());
    dataSourceBuilder.url(ds.getUrl());

    dataSourceBuilder.username(ds.getUsername());
    dataSourceBuilder.password(ds.getPassword());
    return dataSourceBuilder.build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    val vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(true);
    vendorAdapter.setShowSql(false);

    val factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan("de.gematik.erpione.server.jpa");
    factory.setDataSource(dataSource);
    return factory;
  }

  @Bean
  public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    val txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(entityManagerFactory);
    return txManager;
  }

  @Bean
  public SpecificationArgResolverSpringdocOperationCustomizer
      specificationArgResolverSpringdocOperationCustomizer() {
    return new SpecificationArgResolverSpringdocOperationCustomizer();
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new SpecificationArgumentResolver());
  }
}
