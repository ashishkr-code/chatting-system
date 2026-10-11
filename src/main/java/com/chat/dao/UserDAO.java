package com.chat.dao;

import com.chat.models.User;
import com.chat.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.ArrayList;

public class UserDAO {
    
    public static boolean registerUser(String username, String password, String fullName) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            
            Query<Long> query = session.createQuery("SELECT count(u) FROM User u WHERE u.username = :username", Long.class);
            query.setParameter("username", username);
            if (query.uniqueResult() > 0) {
                return false;
            }
            
            User user = new User(username, password, fullName);
            session.persist(user);
            
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        }
    }

    public static User loginUser(String username, String password) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User u WHERE u.username = :username AND u.passwordHash = :password", User.class);
            query.setParameter("username", username);
            query.setParameter("password", password);
            
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static User getUserByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> searchUsers(String searchQuery, String excludeUsername) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User u WHERE u.username LIKE :sq AND u.username != :eu", User.class);
            query.setParameter("sq", "%" + searchQuery + "%");
            query.setParameter("eu", excludeUsername);
            query.setMaxResults(20); // limit to 20 results
            
            List<User> users = query.list();
            List<String> result = new ArrayList<>();
            for (User u : users) {
                result.add(u.getUsername());
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void updateProfilePic(String username, String base64Pic) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            User u = getUserByUsername(username);
            if (u != null) {
                u = session.get(User.class, u.getId());
                u.setProfilePic(base64Pic);
                session.merge(u);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        }
    }

    public static void updateLastSeen(String username) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            User u = getUserByUsername(username);
            if (u != null) {
                u = session.get(User.class, u.getId());
                u.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));
                session.merge(u);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        }
    }
}
