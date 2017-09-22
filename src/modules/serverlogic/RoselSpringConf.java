package modules.serverlogic;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Configuration
@ComponentScan(basePackages = {"modules"})
public class RoselSpringConf {
    
    @Bean
    public SingleConnectionDataSource dataSource(){
        SingleConnectionDataSource ds =  new SingleConnectionDataSource();        
        Properties settings = SettingsManager.getSettings();                
        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");        
        ds.setUrl(settings.getProperty("url"));
        ds.setUsername(settings.getProperty("username"));
        ds.setPassword(settings.getProperty("password"));
        return ds;
    } 
    
    @Bean
    public JdbcTemplate jdbcTemplate(){        
        return new JdbcTemplate(dataSource());
    }
    
    @Bean
    public DataSourceTransactionManager dataSourceTransactionManager(){
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dataSource());
        return dataSourceTransactionManager;
    }
    
}
