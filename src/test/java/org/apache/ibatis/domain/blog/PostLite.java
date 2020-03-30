package org.apache.ibatis.domain.blog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostLite {

    private PostLiteId theId;

    private int blogId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PostLite that = (PostLite) o;

        if (blogId != that.blogId) {
            return false;
        }
        if (theId != null ? !theId.equals(that.theId) : that.theId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int myresult = theId != null ? theId.hashCode() : 0;
        myresult = 31 * myresult + blogId;
        return myresult;
    }
}
