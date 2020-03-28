package org.apache.ibatis.domain.blog;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Author implements Serializable {

    protected int id;

    protected String username;

    protected String password;

    protected String email;

    protected String bio;

    protected Section favouriteSection;

    public Author() {
        this(-1, null, null, null, null, null);
    }

    public Author(Integer id, String username, String password, String email, String bio, Section section) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.bio = bio;
        this.favouriteSection = section;
    }

    public Author(int id) {
        this(id, null, null, null, null, null);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Author)) {
            return false;
        }

        Author author = (Author) o;

        if (id != author.id) {
            return false;
        }
        if (bio != null ? !bio.equals(author.bio) : author.bio != null) {
            return false;
        }
        if (email != null ? !email.equals(author.email) : author.email != null) {
            return false;
        }
        if (password != null ? !password.equals(author.password) : author.password != null) {
            return false;
        }
        if (username != null ? !username.equals(author.username) : author.username != null) {
            return false;
        }
        if (favouriteSection != null ? !favouriteSection.equals(author.favouriteSection) : author.favouriteSection != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = id;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (bio != null ? bio.hashCode() : 0);
        result = 31 * result + (favouriteSection != null ? favouriteSection.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "Author : " + id + " : " + username + " : " + email;
    }
}