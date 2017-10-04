package modules.serverlogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"modules"})
public class RoselSpringConf {
    
    @Bean
    @Autowired
    public SingleConnectionDataSource dataSource(){
        SingleConnectionDataSource ds =  new SingleConnectionDataSource();            
        return ds;
    } 
    
    @Bean
    public JdbcTemplate jdbcTemplate(){        
        return new JdbcTemplate(dataSource());
    }
    
    @Bean
    PlatformTransactionManager transactionManager(){
        return new DataSourceTransactionManager(dataSource()); 
    }   
    
}
