package com.chat.utils;

import com.chat.models.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.io.InputStream;
import java.util.Properties;

public class HibernateUtil {
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Load properties from application.properties
                Properties prop = new Properties();
                try (InputStream input = HibernateUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
                    if (input == null) {
                        System.err.println("Sorry, unable to find application.properties");
                        return null;
                    }
                    prop.load(input);
                }

                Configuration configuration = new Configuration();
                
                // Set Hibernate properties programmatically using values from application.properties
                configuration.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
                configuration.setProperty("hibernate.connection.url", prop.getProperty("db.url"));
                configuration.setProperty("hibernate.connection.username", prop.getProperty("db.username"));
                configuration.setProperty("hibernate.connection.password", prop.getProperty("db.password"));
                configuration.setProperty("hibernate.hbm2ddl.auto", prop.getProperty("hibernate.hbm2ddl.auto"));
                configuration.setProperty("hibernate.show_sql", prop.getProperty("hibernate.show_sql"));
                
                // Add entity classes here
                configuration.addAnnotatedClass(User.class);
                configuration.addAnnotatedClass(com.chat.models.Message.class);
                configuration.addAnnotatedClass(com.chat.models.Friendship.class);

                ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySettings(configuration.getProperties()).build();

                sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sessionFactory;
    }
    
    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
