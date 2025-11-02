package com.logistics.audit;

import org.hibernate.envers.RevisionListener;

public class UserRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity rev = (CustomRevisionEntity) revisionEntity;
        
        // Lấy userId từ context (token)
        Integer currentUserId = CurrentUserContext.getUserId();
        
        // Nếu có userId thì set, không thì để null
        if (currentUserId != null) {
            rev.setUserId(currentUserId);
        } else {
            rev.setUserId(null); 
        }
    }
}