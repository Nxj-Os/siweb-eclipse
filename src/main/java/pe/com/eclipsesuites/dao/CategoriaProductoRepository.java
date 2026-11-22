package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.CategoriaProducto;

import java.util.List;
import java.util.Optional;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Integer> {

    Optional<CategoriaProducto> findByNombreCategoriaIgnoreCase(String nombreCategoria);

    List<CategoriaProducto> findAllByOrderByNombreCategoria();
}
