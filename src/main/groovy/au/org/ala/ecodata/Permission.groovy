package au.org.ala.ecodata

enum Permission {

    READ('read'),
    WRITE('write'),
    CREATE('create'),
    DELETE('delete'),
    UPDATE('update'),
    ADMINISTER('admin'),
    READ_CHILDREN('read_children'),
    UPDATE_CHILDREN('update_children'),
    DELETE_CHILDREN('delete_children')

    final String value

    private Permission(String value) {
        this.value = value
    }

    @Override
    String toString() {
        value
    }

    static Permission fromString(String value) {
        values().find { it.value == value } ?: null
    }
}
