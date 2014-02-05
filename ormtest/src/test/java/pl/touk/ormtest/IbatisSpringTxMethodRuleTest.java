package pl.touk.ormtest;

import com.google.common.base.Joiner;
import org.junit.Test;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class IbatisSpringTxMethodRuleTest {
    @Test
    public void shouldThrowException() throws Exception {
        shouldThrowIllegalArgumentException("sqlMapConfigs must not be empty");
        shouldThrowNullPointerException("sqlMapConfigs[0] is null", new Object[]{null});
        shouldThrowNullPointerException("sqlMapConfigs[0] is null", (Object) null);
        shouldThrowNullPointerException("sqlMapConfigs[1] is null", "foo", null);
        shouldThrowIllegalArgumentException("sqlMapConfigs[1] is an empty string", "foo", "");
        shouldThrowIllegalArgumentException("sqlMapConfigs[1] must be one of: String, Resource, List of two Strings", "foo", new Object());
        shouldThrowIllegalArgumentException("sqlMapConfig[1] is a string list of invalid size (1 but should be 2)",
                "foo", Arrays.asList("foo"));
        shouldThrowIllegalArgumentException("sqlMapConfig[1] is a string list of invalid size (3 but should be 2)",
                "foo", Arrays.asList("foo", "bar", "foobar"));
        shouldThrowNullPointerException("sqlMapConfig[0] is a two-element list with invalid first element (null)",
                Arrays.asList(null, "bar"), "bar");
        shouldThrowNullPointerException("sqlMapConfig[1] is a two-element list with invalid second element (null)",
                "foo", Arrays.asList("foo", null));
        shouldThrowIllegalArgumentException("sqlMapConfig[1] is a two-element list with invalid second element (an empty string)",
                "foo", Arrays.asList("foo", ""));
        shouldThrowIllegalArgumentException("sqlMapConfig[1] is a two-element list with invalid first element (an empty string)",
                "foo", Arrays.asList("", "bar"));
    }

    @Test
    public void shouldAssignResource() throws Exception {
        // given
        Resource resourceMock = mock(Resource.class);

        // when
        IbatisSpringTxMethodRule.setSqlMapConfig(resourceMock);

        // then
        assertThat(IbatisSpringTxMethodRule.getSqlMapConfig()).isEqualTo(new Object[]{resourceMock});
    }

    @Test
    public void shouldAssignString() throws Exception {
        // when
        IbatisSpringTxMethodRule.setSqlMapConfig("foo");

        // then
        assertThat(IbatisSpringTxMethodRule.getSqlMapConfig()).isEqualTo(new Object[]{"foo"});
    }

    @Test
    public void shouldAssignPathAndAncestorDirectory() throws Exception {
        // given
        List<String> pathAndAncestorDirectory = Arrays.asList("path", "descendant dir");

        // when
        IbatisSpringTxMethodRule.setSqlMapConfig(pathAndAncestorDirectory);

        // then
        assertThat(IbatisSpringTxMethodRule.getSqlMapConfig()).isEqualTo(new Object[]{pathAndAncestorDirectory});
    }

    @Test
    public void shouldAssign() throws Exception {
        // given
        String path = "foo";
        List<String> pathAndAncestorDirectory = Arrays.asList("path", "descendant dir");
        Resource resourceMock = mock(Resource.class);

        // when
        IbatisSpringTxMethodRule.setSqlMapConfig(path, pathAndAncestorDirectory, resourceMock);

        // then
        assertThat(IbatisSpringTxMethodRule.getSqlMapConfig()).isEqualTo(new Object[]{path, pathAndAncestorDirectory, resourceMock});
    }

    @Test
    public void shouldCreateSqlMapConfigResourceArrayWithOneResourceFromPath() throws Exception {
        // given
        Resource[] resources;

        // when
        resources = new IbatisSpringTxMethodRule("file:**/src/test/resources/foo.txt").createSqlMapConfigResourceArray();

        // then
        assertThat(resources).hasSize(1);
        assertThat(resources[0].exists()).isTrue();
        assertThat(resources[0].getFile().getAbsolutePath()).contains(path("ormtest", "src", "test", "resources", "foo.txt"));
    }

    @Test
    public void shouldCreateSqlMapConfigResourceArrayWithOneResourceWithoutSpecifyingPattern() throws Exception {
        // given
        Resource[] resources;

        // when
        resources = new IbatisSpringTxMethodRule("classpath:/foo.txt").createSqlMapConfigResourceArray();

        // then
        assertThat(resources).hasSize(1);
        assertThat(resources[0].exists()).isTrue();
        assertThat(resources[0].getFile().getAbsolutePath()).contains("ormtest");
        assertThat(resources[0].getFile().getAbsolutePath()).endsWith("foo.txt");
    }

    @Test
    public void shouldCreateSqlMapConfigResourceArrayWithOneResourceWithoutSpecifyingPatternButWithAncestor() throws Exception {
        // given
        Resource[] resources;

        // when
        resources = new IbatisSpringTxMethodRule("classpath:/foo.txt", "ormtest", "").createSqlMapConfigResourceArray();

        // then
        assertThat(resources).hasSize(1);
        assertThat(resources[0].exists()).isTrue();
        assertThat(resources[0].getFile().getAbsolutePath()).endsWith(path("foo.txt"));
    }

    @Test
    public void shouldCreateSqlMapConfigResourceArrayWithOneResourceWithGivenAncestor() throws Exception {
        // given
        Resource[] resources;

        // when
        resources = new IbatisSpringTxMethodRule("file:**/src/test/resources/foo.txt", "ormtest", "").createSqlMapConfigResourceArray();

        // then
        assertThat(resources).hasSize(1);
        assertThat(resources[0].exists()).isTrue();
        assertThat(resources[0].getFile().getAbsolutePath()).endsWith(path("ormtest", "src", "test", "resources", "foo.txt"));
    }

    @Test
    public void shouldCreateSqlMapConfigResourceArrayWithOneResource() throws Exception {
        // given
        Resource[] resources;
        Resource resourceMock = mock(Resource.class);

        // when
        resources = new IbatisSpringTxMethodRule(resourceMock).createSqlMapConfigResourceArray();

        // then
        assertThat(resources).hasSize(1);
        assertThat(resources[0]).isEqualTo(resourceMock);
    }

    @Test
    public void shouldLoadMainResourceAndNotTheTestOne() throws Exception {
        // given
        Resource[] resources;

        // when
        resources = new IbatisSpringTxMethodRule("file:**/IbatisSpringTxMethodRule.java", "main", "").createSqlMapConfigResourceArray();

        // then
        assertThat(resources).hasSize(1);
        assertThat(resources[0].exists()).isTrue();
        assertThat(resources[0].getFile().getAbsolutePath()).endsWith(
                path("ormtest", "src", "main", "java", "pl", "touk", "ormtest", "IbatisSpringTxMethodRule.java"));
    }

    @Test
    public void shouldThrowExceptionWhileCreateSqlMapConfigResourceArray() throws Exception {
        shouldThrowExceptionWhileCreateSqlMapConfigResourceArray("file:**/src/test/resources/foo.txt", "nonExistingAncestor",
                "no descendant of 'nonExistingAncestor' among resources");
        shouldThrowExceptionWhileCreateSqlMapConfigResourceArray("file:**/src/test/resources/*.txt", null,
                "more than one sqlMapConfig resource found by the given name");
        shouldThrowExceptionWhileCreateSqlMapConfigResourceArray("file:**/src/test/resources/*.txt", "ormtest",
                "resolved to at least two resources containing 'ormtest'");
        shouldThrowExceptionWhileCreateSqlMapConfigResourceArray("file:**/someNonExistingPath", null,
                "can't find resource");
        shouldThrowExceptionWhileCreateSqlMapConfigResourceArray("file:**/someNonExistingPath", "ormtest",
                "can't find resource");
        shouldThrowExceptionWhileCreateSqlMapConfigResourceArray("file:**/IbatisSpringTxMethodRule.java", null,
                "more than one sqlMapConfig resource found by the given name");

    }

    private void shouldThrowExceptionWhileCreateSqlMapConfigResourceArray(String path, String ancestor, String message) throws Exception {
        // given
        RuntimeException thrownException = null;

        // when
        try {
            if (ancestor == null) {
                new IbatisSpringTxMethodRule(path).createSqlMapConfigResourceArray();
            } else {
                new IbatisSpringTxMethodRule(path, ancestor, "").createSqlMapConfigResourceArray();
            }
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            thrownException = e;
        }

        // then
        assertThat(thrownException).isNotNull();
        assertThat(thrownException.getMessage()).contains(message);
    }

    private String path(String... paths) {
        return Joiner.on(File.separator).join(Arrays.asList(paths));
    }

    private void shouldThrowIllegalArgumentException(String exceptionMessage, Object... args) {
        shouldThrow(false, exceptionMessage, args);
    }

    private void shouldThrowNullPointerException(String exceptionMessage, Object... args) {
        shouldThrow(true, exceptionMessage, args);
    }

    private void shouldThrow(boolean nullPointerExceptionExpected, String exceptionMessage, Object... args) {
        try {
            IbatisSpringTxMethodRule.setSqlMapConfig(args);
            fail((nullPointerExceptionExpected ? "NullPointerException" : "IllegalArgumentException")
                    + " expected for IbatisSpringTxMethodRule.setSqlMapConfig(" + Arrays.toString(args) + ")");
        } catch (IllegalArgumentException e) {
            if (nullPointerExceptionExpected) {
                fail("NullPointerException expected for IbatisSpringTxMethodRule.setSqlMapConfig(" + Arrays.deepToString(args) + ") but caught: " + e);
            }
            assertThat(e.getMessage()).isEqualTo(exceptionMessage);
        } catch (NullPointerException e) {
            if (!nullPointerExceptionExpected) {
                fail("IllegalArgumentException expected for IbatisSpringTxMethodRule.setSqlMapConfig(" + Arrays.deepToString(args) + ") but caught: " + e);
            }
            assertThat(e.getMessage()).isEqualTo(exceptionMessage);
        }
    }
}
