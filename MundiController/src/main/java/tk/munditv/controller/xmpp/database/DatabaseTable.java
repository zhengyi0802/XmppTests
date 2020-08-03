package tk.munditv.controller.xmpp.database;

/**
 * @author Aleksandar Gotev
 */
public interface DatabaseTable {
    String getCreateSql();
    String getDropSql();
}
