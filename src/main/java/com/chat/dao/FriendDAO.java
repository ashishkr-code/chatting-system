package com.chat.dao;

import com.chat.models.Friendship;
import com.chat.models.User;
import com.chat.utils.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.ArrayList;

public class FriendDAO {
    
    public static String sendRequest(String requesterUser, String receiverUser) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User requester = UserDAO.getUserByUsername(requesterUser);
            User receiver = UserDAO.getUserByUsername(receiverUser);
            if (requester == null || receiver == null) return "ERROR";
            
            Query<Friendship> q = session.createQuery(
                "FROM Friendship f WHERE (f.requester = :r1 AND f.receiver = :r2) OR (f.requester = :r2 AND f.receiver = :r1)", Friendship.class);
            q.setParameter("r1", requester);
            q.setParameter("r2", receiver);
            
            Friendship existing = q.uniqueResult();
            if (existing != null) {
                if ("BLOCKED".equals(existing.getStatus())) return "BLOCKED";
                return "EXISTS";
            }
            
            tx = session.beginTransaction();
            session.persist(new Friendship(requester, receiver, "PENDING"));
            tx.commit();
            return "SUCCESS";
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            return "ERROR";
        }
    }

    public static boolean acceptRequest(String requesterUser, String receiverUser) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Query<Friendship> q = session.createQuery(
                "FROM Friendship f WHERE f.requester.username = :req AND f.receiver.username = :rec", Friendship.class);
            q.setParameter("req", requesterUser);
            q.setParameter("rec", receiverUser);
            Friendship f = q.uniqueResult();
            
            if (f != null && "PENDING".equals(f.getStatus())) {
                f.setStatus("ACCEPTED");
                session.merge(f);
                tx.commit();
                return true;
            }
            if (tx != null) tx.rollback();
            return false;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            return false;
        }
    }

    public static void blockUser(String requesterUser, String blockedUser) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            User req = UserDAO.getUserByUsername(requesterUser);
            User block = UserDAO.getUserByUsername(blockedUser);
            
            if (req != null && block != null) {
                Query<Friendship> q = session.createQuery(
                    "FROM Friendship f WHERE (f.requester = :r1 AND f.receiver = :r2) OR (f.requester = :r2 AND f.receiver = :r1)", Friendship.class);
                q.setParameter("r1", req);
                q.setParameter("r2", block);
                Friendship f = q.uniqueResult();
                
                if (f != null) {
                    // Update existing friendship to BLOCKED, and set requester as the blocker
                    f.setRequester(req);
                    f.setReceiver(block);
                    f.setStatus("BLOCKED");
                    session.merge(f);
                } else {
                    session.persist(new Friendship(req, block, "BLOCKED"));
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        }
    }

    public static void unblockUser(String requesterUser, String blockedUser) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            User req = UserDAO.getUserByUsername(requesterUser);
            User block = UserDAO.getUserByUsername(blockedUser);
            
            if (req != null && block != null) {
                Query<Friendship> q = session.createQuery(
                    "FROM Friendship f WHERE f.requester = :r1 AND f.receiver = :r2 AND f.status = 'BLOCKED'", Friendship.class);
                q.setParameter("r1", req);
                q.setParameter("r2", block);
                Friendship f = q.uniqueResult();
                
                if (f != null) {
                    session.remove(f); // Completely remove the blocked relationship
                }
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
        }
    }

    public static List<String> getFriends(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Friendship> q = session.createQuery(
                "FROM Friendship f WHERE (f.requester.username = :u OR f.receiver.username = :u) AND f.status = 'ACCEPTED'", Friendship.class);
            q.setParameter("u", username);
            List<Friendship> flist = q.list();
            
            List<String> friends = new ArrayList<>();
            for (Friendship f : flist) {
                if (f.getRequester().getUsername().equals(username)) friends.add(f.getReceiver().getUsername());
                else friends.add(f.getRequester().getUsername());
            }
            return friends;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<String> getPendingRequests(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Friendship> q = session.createQuery(
                "FROM Friendship f WHERE f.receiver.username = :u AND f.status = 'PENDING'", Friendship.class);
            q.setParameter("u", username);
            List<Friendship> flist = q.list();
            
            List<String> requesters = new ArrayList<>();
            for (Friendship f : flist) {
                requesters.add(f.getRequester().getUsername());
            }
            return requesters;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static List<String> getBlockedUsers(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Friendship> q = session.createQuery(
                "FROM Friendship f WHERE f.requester.username = :u AND f.status = 'BLOCKED'", Friendship.class);
            q.setParameter("u", username);
            List<Friendship> flist = q.list();
            
            List<String> blocked = new ArrayList<>();
            for (Friendship f : flist) {
                blocked.add(f.getReceiver().getUsername());
            }
            return blocked;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
