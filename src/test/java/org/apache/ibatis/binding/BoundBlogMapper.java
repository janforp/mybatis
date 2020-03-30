package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.TypeDiscriminator;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.Blog;
import org.apache.ibatis.domain.blog.DraftPost;
import org.apache.ibatis.domain.blog.Post;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

@CacheNamespace(readWrite = false)
public interface BoundBlogMapper {

    //======================================================

    Blog selectBlogWithPostsUsingSubSelect(int id);

    //======================================================

    int selectRandom();

    //======================================================

    @Select({ "SELECT * FROM blog" })
    @MapKey("id")
    Map<Integer, Blog> selectBlogsAsMapById();

    //======================================================

    @Select({
            "SELECT *",
            "FROM blog"
    })
    List<Blog> selectBlogs();

    //======================================================

    List<Blog> selectBlogsFromXML();

    //======================================================

    @Select({
            "SELECT *",
            "FROM blog"
    })
    List<Map<String, Object>> selectBlogsAsMaps();

    //======================================================

    @SelectProvider(type = BoundBlogSql.class, method = "selectBlogsSql")
    List<Blog> selectBlogsUsingProvider();

    //======================================================

    @Select("SELECT * FROM post ORDER BY id")
    @TypeDiscriminator(
            column = "draft",
            javaType = String.class,
            cases = { @Case(value = "1", type = DraftPost.class) }
    )
    List<Post> selectPosts();

    //======================================================

    @Select("SELECT * FROM post ORDER BY id")
    @Results({
            @Result(id = true, property = "id", column = "id")
    })
    @TypeDiscriminator(
            column = "draft",
            javaType = int.class,
            cases = { @Case(value = "1", type = DraftPost.class,
                    results = { @Result(id = true, property = "id", column = "id") }) }
    )
    List<Post> selectPostsWithResultMap();

    //======================================================

    @Select("SELECT * FROM " +
            "blog WHERE id = #{id}")
    Blog selectBlog(int id);

    //======================================================

    @Select("SELECT * FROM " +
            "blog WHERE id = #{id}")
    @ConstructorArgs({
            @Arg(column = "id", javaType = int.class, id = true),
            @Arg(column = "title", javaType = String.class),
            @Arg(column = "author_id", javaType = Author.class, select = "org.apache.ibatis.binding.BoundAuthorMapper.selectAuthor"),
            @Arg(column = "id", javaType = List.class, select = "selectPostsForBlog")
    })
    Blog selectBlogUsingConstructor(int id);

    Blog selectBlogUsingConstructorWithResultMap(int i);

    Blog selectBlogUsingConstructorWithResultMapAndProperties(int i);

    Blog selectBlogUsingConstructorWithResultMapCollection(int i);

    Blog selectBlogByIdUsingConstructor(int id);

    //======================================================

    @Select("SELECT * FROM " +
            "blog WHERE id = #{id}")
    Map<String, Object> selectBlogAsMap(Map<String, Object> params);

    //======================================================

    @Select("SELECT * FROM " +
            "post WHERE subject like #{query}")
    List<Post> selectPostsLike(RowBounds bounds, String query);

    //======================================================

    @Select("SELECT * FROM " +
            "post WHERE subject like #{subjectQuery} and body like #{bodyQuery}")
    List<Post> selectPostsLikeSubjectAndBody(
            @Param("subjectQuery") String subjectQuery,
            @Param("bodyQuery") String bodyQuery, RowBounds bounds);

    //======================================================

    @Select("SELECT * FROM " +
            "post WHERE id = #{id}")
    List<Post> selectPostsById(int id);

    //======================================================

    @Select("SELECT * FROM blog " +
            "WHERE id = #{id} AND title = #{nonExistentParam,jdbcType=VARCHAR}")
    Blog selectBlogByNonExistentParam(@Param("id") int id);

    @Select("SELECT * FROM blog " +
            "WHERE id = #{id} AND title = #{params.nonExistentParam,jdbcType=VARCHAR}")
    Blog selectBlogByNonExistentNestedParam(@Param("id") int id, @Param("params") Map<String, Object> params);

    @Select("SELECT * FROM blog WHERE id = #{id}")
    Blog selectBlogByNullParam(Integer id);

    //======================================================

    @Select("SELECT * FROM blog " +
            "WHERE id = #{0} AND title = #{1}")
    Blog selectBlogByDefault30ParamNames(int id, String title);

    @Select("SELECT * FROM blog " +
            "WHERE id = #{param1} AND title = #{param2}")
    Blog selectBlogByDefault31ParamNames(int id, String title);

    //======================================================

    @Select("SELECT * FROM blog " +
            "WHERE ${column} = #{id} AND title = #{value}")
    Blog selectBlogWithAParamNamedValue(@Param("column") String column, @Param("id") int id, @Param("value") String title);

    //======================================================

    @Select({
            "SELECT *",
            "FROM blog"
    })
    @Results({
            @Result(property = "author", column = "author_id", one = @One(select = "org.apache.ibatis.binding.BoundAuthorMapper.selectAuthor")),
            @Result(property = "posts", column = "id", many = @Many(select = "selectPostsById"))
    })
    List<Blog> selectBlogsWithAutorAndPosts();

    @Select({
            "SELECT *",
            "FROM blog"
    })
    @Results({
            @Result(property = "author", column = "author_id", one = @One(select = "org.apache.ibatis.binding.BoundAuthorMapper.selectAuthor", fetchType = FetchType.EAGER)),
            @Result(property = "posts", column = "id", many = @Many(select = "selectPostsById", fetchType = FetchType.EAGER))
    })
    List<Blog> selectBlogsWithAutorAndPostsEagerly();
}
