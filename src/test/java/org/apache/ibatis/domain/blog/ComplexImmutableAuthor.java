package org.apache.ibatis.domain.blog;

import lombok.Getter;

import java.io.Serializable;

public class ComplexImmutableAuthor implements Serializable {

    @Getter
    protected final String bio;

    @Getter
    protected final Section favouriteSection;

    @Getter
    private final ComplexImmutableAuthorId theComplexImmutableAuthorId;

    public ComplexImmutableAuthor(ComplexImmutableAuthorId aComplexImmutableAuthorId, String bio, Section section) {
        theComplexImmutableAuthorId = aComplexImmutableAuthorId;
        this.bio = bio;
        this.favouriteSection = section;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ComplexImmutableAuthor that = (ComplexImmutableAuthor) o;

        if (bio != null ? !bio.equals(that.bio) : that.bio != null) {
            return false;
        }
        if (favouriteSection != that.favouriteSection) {
            return false;
        }
        if (theComplexImmutableAuthorId != null ? !theComplexImmutableAuthorId.equals(that.theComplexImmutableAuthorId) : that.theComplexImmutableAuthorId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int myresult = theComplexImmutableAuthorId != null ? theComplexImmutableAuthorId.hashCode() : 0;
        myresult = 31 * myresult + (bio != null ? bio.hashCode() : 0);
        myresult = 31 * myresult + (favouriteSection != null ? favouriteSection.hashCode() : 0);
        return myresult;
    }
}
