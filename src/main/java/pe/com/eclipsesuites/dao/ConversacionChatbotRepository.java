package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.eclipsesuites.entity.ConversacionChatbot;

import java.util.List;
import java.util.Optional;

public interface ConversacionChatbotRepository extends JpaRepository<ConversacionChatbot, Long> {

    Optional<ConversacionChatbot> findByIdSesionWeb(String idSesionWeb);

    List<ConversacionChatbot> findByIdCliente(Integer idCliente);

    boolean existsByIdSesionWeb(String idSesionWeb);
}
