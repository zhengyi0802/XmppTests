package tk.munditv.mundichat.database;

/**
 * @author Aleksandar Gotev
 */
public interface DatabaseTable {
    String getCreateSql();
    String getDropSql();
}
