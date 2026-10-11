package com.chat.dao;

import com.chat.models.Message;
import com.chat.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.List;

public class MessageDAO {
    
    public static int saveMessage(Message message) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(message);
            transaction.commit();
            return message.getId(); // Return the DB generated ID
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return -1;
        }
    }

    public static void deleteMessage(int messageId, String requesterUsername) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Message msg = session.get(Message.class, messageId);
            if (msg != null && msg.getSender().getUsername().equals(requesterUsername)) {
                session.remove(msg);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
        }
    }

    public static void addReaction(int messageId, String username, String reaction) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Message msg = session.get(Message.class, messageId);
            if (msg != null) {
                String existing = msg.getReactions();
                com.google.gson.Gson gson = new com.google.gson.Gson();
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
                java.util.Map<String, String> map = gson.fromJson(existing, type);
                if (map == null) map = new java.util.HashMap<>();
                map.put(username, reaction);
                msg.setReactions(gson.toJson(map));
                session.merge(msg);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        }
    }

    public static List<Message> getGlobalChatHistory() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Message m WHERE m.receiver IS NULL ORDER BY m.id ASC", Message.class)
                    .setMaxResults(100)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static List<Message> getPrivateChatHistory(String user1, String user2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Message> query = session.createQuery(
                "FROM Message m WHERE (m.sender.username = :u1 AND m.receiver.username = :u2) " +
                "OR (m.sender.username = :u2 AND m.receiver.username = :u1) ORDER BY m.id ASC", Message.class);
            query.setParameter("u1", user1);
            query.setParameter("u2", user2);
            return query.setMaxResults(100).list();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static void updateMessageStatus(int messageId, String newStatus) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Message msg = session.get(Message.class, messageId);
            if (msg != null) {
                msg.setMessageStatus(newStatus);
                session.merge(msg);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
        }
    }
}
