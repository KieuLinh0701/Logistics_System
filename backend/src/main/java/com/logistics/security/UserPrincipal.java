package com.logistics.security;

import com.logistics.entity.User;
import com.logistics.entity.Account;
import com.logistics.entity.Role;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserPrincipal implements UserDetails {

    private final Account account;
    private final User user;
    private final Role currentRole;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Account account, User user, Role currentRole, Collection<? extends GrantedAuthority> authorities) {
        this.account = account;
        this.user = user;
        this.currentRole = currentRole;
        this.authorities = authorities;
    }

    public Account getAccount() {
        return account;
    }

    public User getUser() {
        return user;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return account.getPassword(); 
    }

    @Override
    public String getUsername() {
        return account.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}