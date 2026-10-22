package pe.com.eclipsesuites.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpleadoEntityStructureTest {

    @Test
    void empleadoShouldBeJpaEntity() {
        assertTrue(Empleado.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void rolEmpleadoShouldBeJpaEntity() {
        assertTrue(RolEmpleado.class.isAnnotationPresent(Entity.class));
    }

    @Test
    void empleadoShouldMapearTablaEmpleado() {
        Table tabla = Empleado.class.getAnnotation(Table.class);
        assertNotNull(tabla);
        assertEquals("empleado", tabla.name());
    }

    @Test
    void rolEmpleadoShouldMapearTablaRolEmpleado() {
        Table tabla = RolEmpleado.class.getAnnotation(Table.class);
        assertNotNull(tabla);
        assertEquals("rol_empleado", tabla.name());
    }

    @Test
    void empleadoShouldTenerCamposEsperados() throws NoSuchFieldException {
        assertNotNull(Empleado.class.getDeclaredField("id"));
        assertNotNull(Empleado.class.getDeclaredField("rol"));
        assertNotNull(Empleado.class.getDeclaredField("nombre"));
        assertNotNull(Empleado.class.getDeclaredField("apellido"));
        assertNotNull(Empleado.class.getDeclaredField("edad"));
        assertNotNull(Empleado.class.getDeclaredField("usernameLogin"));
        assertNotNull(Empleado.class.getDeclaredField("passwordHashBcrypt"));
        assertNotNull(Empleado.class.getDeclaredField("turnoLaboral"));
        assertNotNull(Empleado.class.getDeclaredField("estadoActivo"));
    }

    @Test
    void rolEmpleadoShouldTenerCamposEsperados() throws NoSuchFieldException {
        assertNotNull(RolEmpleado.class.getDeclaredField("id"));
        assertNotNull(RolEmpleado.class.getDeclaredField("nombreRol"));
        assertNotNull(RolEmpleado.class.getDeclaredField("permisosSistema"));
    }

    @Test
    void usernameLoginShouldBeUnique() throws NoSuchFieldException {
        Field campo = Empleado.class.getDeclaredField("usernameLogin");
        Column column = campo.getAnnotation(Column.class);
        assertNotNull(column);
        assertTrue(column.unique());
        assertEquals("username_login", column.name());
        assertEquals(50, column.length());
    }

    @Test
    void passwordHashShouldMapColumnPasswordHashBcrypt() throws NoSuchFieldException {
        Field campo = Empleado.class.getDeclaredField("passwordHashBcrypt");
        Column column = campo.getAnnotation(Column.class);
        assertNotNull(column);
        assertEquals("password_hash_bcrypt", column.name());
        assertEquals(100, column.length());
    }

    @Test
    void nombreRolShouldBeUnique() throws NoSuchFieldException {
        Field campo = RolEmpleado.class.getDeclaredField("nombreRol");
        Column column = campo.getAnnotation(Column.class);
        assertNotNull(column);
        assertTrue(column.unique());
        assertEquals("nombre_rol", column.name());
    }

    @Test
    void empleadoShouldRelacionarseConRolEmpleado() throws NoSuchFieldException {
        Field campo = Empleado.class.getDeclaredField("rol");
        ManyToOne manyToOne = campo.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());
        JoinColumn join = campo.getAnnotation(JoinColumn.class);
        assertNotNull(join);
        assertEquals("id_rol", join.name());
    }
}
