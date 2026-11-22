package pe.com.eclipsesuites.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.eclipsesuites.entity.MensajeChatbot;

import java.util.List;

public interface MensajeChatbotRepository extends JpaRepository<MensajeChatbot, Long> {

    @Query("select m from MensajeChatbot m where m.idConversacion = :idConversacion "
            + "order by m.fhMensaje asc, m.id asc")
    List<MensajeChatbot> findByConversacion(@Param("idConversacion") Long idConversacion);

    long countByIdConversacionAndRolEmisor(Long idConversacion, String rolEmisor);
}
