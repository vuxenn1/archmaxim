package db.migration;

import com.archmaxim.search.TextNormalizer;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Fills course_alias.normalized_alias for existing rows and makes it unique.
 * Written in Java so the values match {@link TextNormalizer} exactly. When two
 * aliases normalize to the same value, the older one is kept.
 */
public class V3__NormalizeCourseAliases extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Set<String> seen = new HashSet<>();

        try (Statement select = connection.createStatement();
             ResultSet aliases = select.executeQuery("SELECT id, alias FROM course_alias ORDER BY id");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE course_alias SET normalized_alias = ? WHERE id = ?");
             PreparedStatement delete = connection.prepareStatement("DELETE FROM course_alias WHERE id = ?")) {

            while (aliases.next()) {
                long id = aliases.getLong("id");
                String normalized = TextNormalizer.compact(aliases.getString("alias"));
                if (normalized.isEmpty() || !seen.add(normalized)) {
                    delete.setLong(1, id);
                    delete.addBatch();
                } else {
                    update.setString(1, normalized);
                    update.setLong(2, id);
                    update.addBatch();
                }
            }
            delete.executeBatch();
            update.executeBatch();
        }

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE course_alias ALTER COLUMN normalized_alias SET NOT NULL");
            ddl.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_course_alias_normalized "
                    + "ON course_alias (normalized_alias)");
        }
    }
}
