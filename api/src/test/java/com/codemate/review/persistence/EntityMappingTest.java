package com.codemate.review.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.codemate.review.persistence.entity.RepositoryEntity;
import com.codemate.review.persistence.entity.ReviewCommentEntity;
import com.codemate.review.persistence.entity.ReviewEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class EntityMappingTest {

    @Test
    void repositoryEntityMapsToCorrectTable() {
        assertThat(RepositoryEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(RepositoryEntity.class.getAnnotation(Table.class).name()).isEqualTo("repositories");
    }

    @Test
    void reviewEntityMapsToCorrectTable() {
        assertThat(ReviewEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ReviewEntity.class.getAnnotation(Table.class).name()).isEqualTo("reviews");
    }

    @Test
    void reviewCommentEntityMapsToCorrectTable() {
        assertThat(ReviewCommentEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ReviewCommentEntity.class.getAnnotation(Table.class).name()).isEqualTo("review_comments");
    }

    @Test
    void entitiesAreConstructibleAndSettable() {
        var r = new RepositoryEntity();
        r.setFullName("o/r");
        assertThat(r.getFullName()).isEqualTo("o/r");

        var rv = new ReviewEntity();
        rv.setStatus("running");
        assertThat(rv.getStatus()).isEqualTo("running");

        var c = new ReviewCommentEntity();
        c.setLine(42);
        assertThat(c.getLine()).isEqualTo(42);
    }
}
