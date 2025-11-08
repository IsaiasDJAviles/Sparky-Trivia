-- ============================================================================
-- SPARKY TRIVIA - SCHEMA SQL CON TU NOMENCLATURA
-- Base de datos: PostgreSQL 14+
-- Nomenclatura: Según tu diagrama (camelCase para campos, PascalCase para tablas)
-- ============================================================================

-- Habilitar extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ELIMINAR TABLAS EXISTENTES (para desarrollo)
-- ============================================================================

DROP TABLE IF EXISTS RespuestasJugador CASCADE;
DROP TABLE IF EXISTS Participantes CASCADE;
DROP TABLE IF EXISTS Sala CASCADE;
DROP TABLE IF EXISTS OpcionesRespuesta CASCADE;
DROP TABLE IF EXISTS Preguntas CASCADE;
DROP TABLE IF EXISTS Trivia CASCADE;
DROP TABLE IF EXISTS Usuario CASCADE;

-- Eliminar funciones
DROP FUNCTION IF EXISTS generarCodigoSala() CASCADE;
DROP FUNCTION IF EXISTS actualizarFechaActualizacion() CASCADE;
DROP FUNCTION IF EXISTS actualizarContadorPreguntas() CASCADE;
DROP FUNCTION IF EXISTS actualizarContadorParticipantes() CASCADE;

-- ============================================================================
-- TABLA: Usuario
-- ============================================================================

CREATE TABLE Usuario (
    usuarioID               SERIAL PRIMARY KEY,
    email                   VARCHAR(255) UNIQUE NOT NULL,
    passwordHash            VARCHAR(255) NOT NULL, -- ⭐ AGREGADO (CRÍTICO)
    firstName               VARCHAR(100) NOT NULL,
    lastName                VARCHAR(100) NOT NULL,
    nickName                VARCHAR(50) UNIQUE NOT NULL,
    DOB                     DATE,
    FotoPerfil              VARCHAR(500),
    Rol                     VARCHAR(20) DEFAULT 'player' CHECK (Rol IN ('player', 'host', 'admin')),
    Status                  VARCHAR(20) DEFAULT 'activo' CHECK (Status IN ('activo', 'eliminado', 'baneado')),
    fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fechaActualizacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fechaLogin              TIMESTAMP,
    
    CONSTRAINT email_valido CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT nickname_minimo CHECK (LENGTH(nickName) >= 3)
);

COMMENT ON TABLE Usuario IS 'Usuarios registrados en la plataforma Sparky Trivia';
COMMENT ON COLUMN Usuario.passwordHash IS 'Hash BCrypt del password (nunca texto plano)';
COMMENT ON COLUMN Usuario.Status IS 'activo = cuenta activa, eliminado = soft delete, baneado = suspendido';

-- Índices para Usuario
CREATE INDEX idx_usuario_email ON Usuario(email);
CREATE INDEX idx_usuario_nickname ON Usuario(nickName);
CREATE INDEX idx_usuario_status ON Usuario(Status);
CREATE INDEX idx_usuario_rol ON Usuario(Rol);

-- ============================================================================
-- TABLA: Trivia
-- ============================================================================

CREATE TABLE Trivia (
    triviaID                SERIAL PRIMARY KEY,
    titulo                  VARCHAR(255) NOT NULL,
    descripcion             TEXT,
    FKHostUser              INTEGER NOT NULL REFERENCES Usuario(usuarioID) ON DELETE CASCADE,
    categoria               VARCHAR(50) DEFAULT 'general',
    dificultad              VARCHAR(20) DEFAULT 'medio' CHECK (dificultad IN ('facil', 'medio', 'dificil')),
    preguntasTotales        INTEGER DEFAULT 0,
    tiempoEstimado          INTEGER DEFAULT 15, -- en minutos
    fotoPortada             VARCHAR(500),
    esPublico               BOOLEAN DEFAULT TRUE, -- ⭐ AGREGADO (RECOMENDADO)
    Status                  VARCHAR(20) DEFAULT 'borrador' CHECK (Status IN ('borrador', 'activo', 'archivado')),
    fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fechaActualizacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    vecesJugada             INTEGER DEFAULT 0, -- ⭐ RENOMBRADO (era jugadoresActuales)
    
    CONSTRAINT tiempo_valido CHECK (tiempoEstimado > 0),
    CONSTRAINT preguntas_validas CHECK (preguntasTotales >= 0)
);

COMMENT ON TABLE Trivia IS 'Banco de trivias creadas por los hosts';
COMMENT ON COLUMN Trivia.Status IS 'borrador = en construcción, activo = publicada, archivado = descontinuada';
COMMENT ON COLUMN Trivia.vecesJugada IS 'Contador de cuántas veces se ha jugado esta trivia';
COMMENT ON COLUMN Trivia.esPublico IS 'true = visible para todos, false = solo accesible con código';

-- Índices para Trivia
CREATE INDEX idx_trivia_host ON Trivia(FKHostUser);
CREATE INDEX idx_trivia_categoria ON Trivia(categoria);
CREATE INDEX idx_trivia_status ON Trivia(Status);
CREATE INDEX idx_trivia_publico ON Trivia(esPublico);
CREATE INDEX idx_trivia_popularidad ON Trivia(vecesJugada DESC);

-- ============================================================================
-- TABLA: Preguntas
-- ============================================================================

CREATE TABLE Preguntas (
    preguntaID              SERIAL PRIMARY KEY,
    FKTrivia                INTEGER NOT NULL REFERENCES Trivia(triviaID) ON DELETE CASCADE,
    orderPregunta           INTEGER NOT NULL,
    contenido               TEXT NOT NULL,
    tipo                    VARCHAR(20) DEFAULT 'opcion_multiple' CHECK (tipo IN ('opcion_multiple', 'verdadero_falso', 'abierta')),
    puntos                  INTEGER DEFAULT 100, -- ⭐ AGREGADO (CRÍTICO)
    limiteTiempo            INTEGER DEFAULT 30, -- segundos
    dificultad              VARCHAR(20) DEFAULT 'medio' CHECK (dificultad IN ('facil', 'medio', 'dificil')),
    imagenPregunta          VARCHAR(500),
    explicacion             TEXT,
    fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT orden_unico UNIQUE(FKTrivia, orderPregunta),
    CONSTRAINT puntos_validos CHECK (puntos > 0),
    CONSTRAINT tiempo_valido CHECK (limiteTiempo BETWEEN 5 AND 300),
    CONSTRAINT contenido_minimo CHECK (LENGTH(contenido) >= 10)
);

COMMENT ON TABLE Preguntas IS 'Preguntas que componen cada trivia';
COMMENT ON COLUMN Preguntas.orderPregunta IS 'Orden de aparición (1, 2, 3, ...)';
COMMENT ON COLUMN Preguntas.puntos IS 'Puntos base que vale esta pregunta';
COMMENT ON COLUMN Preguntas.explicacion IS 'Explicación de la respuesta correcta (modo educativo)';

-- Índices para Preguntas
CREATE INDEX idx_preguntas_trivia ON Preguntas(FKTrivia);
CREATE INDEX idx_preguntas_orden ON Preguntas(FKTrivia, orderPregunta);

-- ============================================================================
-- TABLA: OpcionesRespuesta
-- ============================================================================

CREATE TABLE OpcionesRespuesta (
    opcionID                SERIAL PRIMARY KEY,
    FKPregunta              INTEGER NOT NULL REFERENCES Preguntas(preguntaID) ON DELETE CASCADE,
    orderPregunta           INTEGER NOT NULL,
    textoOpcion             TEXT NOT NULL, -- ⭐ AGREGADO (CRÍTICO)
    isCorrecto              BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT orden_opcion_unico UNIQUE(FKPregunta, orderPregunta),
    CONSTRAINT orden_valido CHECK (orderPregunta BETWEEN 1 AND 6),
    CONSTRAINT texto_minimo CHECK (LENGTH(textoOpcion) >= 1)
);

COMMENT ON TABLE OpcionesRespuesta IS 'Opciones de respuesta para cada pregunta';
COMMENT ON COLUMN OpcionesRespuesta.orderPregunta IS 'Orden de la opción: 1=A, 2=B, 3=C, 4=D';
COMMENT ON COLUMN OpcionesRespuesta.textoOpcion IS 'Texto de la opción (ej: "París", "Londres")';
COMMENT ON COLUMN OpcionesRespuesta.isCorrecto IS 'true = respuesta correcta, false = incorrecta';

-- Índices para OpcionesRespuesta
CREATE INDEX idx_opciones_pregunta ON OpcionesRespuesta(FKPregunta);
CREATE INDEX idx_opciones_correcta ON OpcionesRespuesta(FKPregunta, isCorrecto);

-- ============================================================================
-- TABLA: Sala
-- ============================================================================

CREATE TABLE Sala (
    salaID                  SERIAL PRIMARY KEY,
    codigoSala              VARCHAR(10) UNIQUE NOT NULL,
    nombreSala              VARCHAR(255),
    FKTrivia                INTEGER NOT NULL REFERENCES Trivia(triviaID) ON DELETE RESTRICT,
    FKUsuario               INTEGER NOT NULL REFERENCES Usuario(usuarioID) ON DELETE CASCADE,
    max_usuario             INTEGER DEFAULT 50,
    usuariosActuales        INTEGER DEFAULT 0,
    status                  VARCHAR(20) DEFAULT 'esperando' CHECK (status IN ('esperando', 'en_progreso', 'completada', 'cancelada')),
    preguntaActual          INTEGER DEFAULT 0,
    esPublico               BOOLEAN DEFAULT TRUE,
    unirseDespues           BOOLEAN DEFAULT FALSE,
    fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    inicio                  TIMESTAMP,
    finalizacion            TIMESTAMP,
    
    CONSTRAINT usuarios_validos CHECK (usuariosActuales <= max_usuario),
    CONSTRAINT max_usuarios_valido CHECK (max_usuario BETWEEN 1 AND 100)
);

COMMENT ON TABLE Sala IS 'Salas donde se juegan las trivias en tiempo real';
COMMENT ON COLUMN Sala.codigoSala IS 'Código único de 6 caracteres para unirse (ej: ABC123)';
COMMENT ON COLUMN Sala.status IS 'esperando = lobby, en_progreso = jugando, completada = terminado';
COMMENT ON COLUMN Sala.preguntaActual IS 'Índice de la pregunta actual que todos ven (0 = no iniciado)';
COMMENT ON COLUMN Sala.unirseDespues IS 'Permitir que jugadores se unan después de iniciar';

-- Índices para Sala
CREATE INDEX idx_sala_codigo ON Sala(codigoSala);
CREATE INDEX idx_sala_status ON Sala(status);
CREATE INDEX idx_sala_trivia ON Sala(FKTrivia);
CREATE INDEX idx_sala_host ON Sala(FKUsuario);
CREATE INDEX idx_sala_creacion ON Sala(fechaCreacion DESC);

-- ============================================================================
-- TABLA: Participantes
-- ============================================================================

CREATE TABLE Participantes (
    participanteID          SERIAL PRIMARY KEY,
    FKSala                  INTEGER NOT NULL REFERENCES Sala(salaID) ON DELETE CASCADE,
    FKUsuario               INTEGER NOT NULL REFERENCES Usuario(usuarioID) ON DELETE CASCADE,
    nicknameJuego           VARCHAR(50), -- ⭐ RENOMBRADO (era nickname-Juego, guión no es válido)
    unio                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    abandono                TIMESTAMP,
    esActivo                BOOLEAN DEFAULT TRUE,
    esHost                  BOOLEAN DEFAULT FALSE,
    puntajeFinal            INTEGER DEFAULT 0,
    rangoFinal              INTEGER,
    preguntaRespuesta       INTEGER DEFAULT 0,
    preguntaCorrecta        INTEGER DEFAULT 0, -- ⭐ CORREGIDO (era peguntaCorrecta - typo)
    
    CONSTRAINT usuario_unico_sala UNIQUE(FKSala, FKUsuario),
    CONSTRAINT puntaje_valido CHECK (puntajeFinal >= 0),
    CONSTRAINT respuestas_validas CHECK (preguntaCorrecta <= preguntaRespuesta)
);

COMMENT ON TABLE Participantes IS 'Registro de jugadores en cada sala';
COMMENT ON COLUMN Participantes.nicknameJuego IS 'Nickname mostrado en esta partida específica';
COMMENT ON COLUMN Participantes.esHost IS 'true = anfitrión de la sala, false = jugador regular';
COMMENT ON COLUMN Participantes.puntajeFinal IS 'Puntaje total acumulado (se actualiza en tiempo real)';
COMMENT ON COLUMN Participantes.preguntaRespuesta IS 'Contador de preguntas respondidas';
COMMENT ON COLUMN Participantes.preguntaCorrecta IS 'Contador de respuestas correctas';

-- Índices para Participantes
CREATE INDEX idx_participantes_sala ON Participantes(FKSala);
CREATE INDEX idx_participantes_usuario ON Participantes(FKUsuario);
CREATE INDEX idx_participantes_activo ON Participantes(FKSala, esActivo);
CREATE INDEX idx_participantes_ranking ON Participantes(FKSala, rangoFinal);
CREATE INDEX idx_participantes_puntaje ON Participantes(FKSala, puntajeFinal DESC);

-- ============================================================================
-- TABLA: RespuestasJugador
-- ============================================================================

CREATE TABLE RespuestasJugador (
    respuestaID             SERIAL PRIMARY KEY,
    FKParticipante          INTEGER NOT NULL REFERENCES Participantes(participanteID) ON DELETE CASCADE,
    FKPregunta              INTEGER NOT NULL REFERENCES Preguntas(preguntaID) ON DELETE CASCADE,
    FKSala                  INTEGER NOT NULL REFERENCES Sala(salaID) ON DELETE CASCADE,
    opcionSeleccionada      INTEGER REFERENCES OpcionesRespuesta(opcionID) ON DELETE SET NULL,
    esCorrecta              BOOLEAN,
    tiempoTomado            INTEGER, -- segundos
    puntosGanados           INTEGER DEFAULT 0,
    respondioEn             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT respuesta_unica UNIQUE(FKParticipante, FKPregunta),
    CONSTRAINT tiempo_valido CHECK (tiempoTomado >= 0),
    CONSTRAINT puntos_validos CHECK (puntosGanados >= 0)
);

COMMENT ON TABLE RespuestasJugador IS 'Registro de todas las respuestas dadas por jugadores';
COMMENT ON COLUMN RespuestasJugador.tiempoTomado IS 'Segundos que tardó en responder';
COMMENT ON COLUMN RespuestasJugador.puntosGanados IS 'Puntos otorgados (varía según velocidad)';

-- Índices para RespuestasJugador
CREATE INDEX idx_respuestas_participante ON RespuestasJugador(FKParticipante);
CREATE INDEX idx_respuestas_sala ON RespuestasJugador(FKSala);
CREATE INDEX idx_respuestas_pregunta ON RespuestasJugador(FKPregunta);
CREATE INDEX idx_respuestas_correcta ON RespuestasJugador(esCorrecta);

-- ============================================================================
-- FUNCIONES AUXILIARES
-- ============================================================================

-- Función: Generar código único para salas
CREATE OR REPLACE FUNCTION generarCodigoSala()
RETURNS VARCHAR(10) AS $$
DECLARE
    codigo VARCHAR(10);
    existe BOOLEAN;
BEGIN
    LOOP
        -- Generar código alfanumérico de 6 caracteres mayúsculas
        codigo := UPPER(substring(md5(random()::text || clock_timestamp()::text) from 1 for 6));
        
        -- Verificar si ya existe
        SELECT EXISTS(SELECT 1 FROM Sala WHERE codigoSala = codigo) INTO existe;
        
        EXIT WHEN NOT existe;
    END LOOP;
    
    RETURN codigo;
END;
$$ LANGUAGE plpgsql;

-- Función: Actualizar timestamp fechaActualizacion automáticamente
CREATE OR REPLACE FUNCTION actualizarFechaActualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fechaActualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Función: Actualizar contador de preguntas en trivia
CREATE OR REPLACE FUNCTION actualizarContadorPreguntas()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE Trivia 
        SET preguntasTotales = preguntasTotales + 1 
        WHERE triviaID = NEW.FKTrivia;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE Trivia 
        SET preguntasTotales = preguntasTotales - 1 
        WHERE triviaID = OLD.FKTrivia;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Función: Actualizar contador de participantes en sala
CREATE OR REPLACE FUNCTION actualizarContadorParticipantes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.esActivo = true THEN
        UPDATE Sala 
        SET usuariosActuales = usuariosActuales + 1 
        WHERE salaID = NEW.FKSala;
    ELSIF TG_OP = 'UPDATE' AND OLD.esActivo = true AND NEW.esActivo = false THEN
        UPDATE Sala 
        SET usuariosActuales = usuariosActuales - 1 
        WHERE salaID = NEW.FKSala;
    ELSIF TG_OP = 'DELETE' AND OLD.esActivo = true THEN
        UPDATE Sala 
        SET usuariosActuales = usuariosActuales - 1 
        WHERE salaID = OLD.FKSala;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

-- Trigger: Actualizar fechaActualizacion en Usuario
CREATE TRIGGER trigger_usuario_actualizacion
    BEFORE UPDATE ON Usuario
    FOR EACH ROW
    EXECUTE FUNCTION actualizarFechaActualizacion();

-- Trigger: Actualizar fechaActualizacion en Trivia
CREATE TRIGGER trigger_trivia_actualizacion
    BEFORE UPDATE ON Trivia
    FOR EACH ROW
    EXECUTE FUNCTION actualizarFechaActualizacion();

-- Trigger: Actualizar contador de preguntas en trivias
CREATE TRIGGER trigger_contador_preguntas
    AFTER INSERT OR DELETE ON Preguntas
    FOR EACH ROW
    EXECUTE FUNCTION actualizarContadorPreguntas();

-- Trigger: Actualizar contador de participantes en salas
CREATE TRIGGER trigger_contador_participantes
    AFTER INSERT OR UPDATE OR DELETE ON Participantes
    FOR EACH ROW
    EXECUTE FUNCTION actualizarContadorParticipantes();

-- ============================================================================
-- VISTAS ÚTILES
-- ============================================================================

-- Vista: Ranking en tiempo real por sala
CREATE OR REPLACE VIEW v_RankingEnVivo AS
SELECT 
    p.FKSala,
    p.participanteID,
    u.usuarioID,
    COALESCE(p.nicknameJuego, u.nickName) as nombreMostrar,
    u.FotoPerfil,
    p.puntajeFinal as puntajeActual,
    p.preguntaCorrecta,
    p.preguntaRespuesta,
    RANK() OVER (PARTITION BY p.FKSala ORDER BY p.puntajeFinal DESC, p.unio ASC) as rankingActual
FROM Participantes p
JOIN Usuario u ON p.FKUsuario = u.usuarioID
WHERE p.esActivo = true
ORDER BY p.FKSala, rankingActual;

COMMENT ON VIEW v_RankingEnVivo IS 'Ranking en tiempo real de jugadores en cada sala';

-- Vista: Estadísticas de usuario
CREATE OR REPLACE VIEW v_EstadisticasUsuario AS
SELECT 
    u.usuarioID,
    u.nickName,
    u.FotoPerfil,
    COUNT(DISTINCT p.FKSala) as partidasJugadas,
    SUM(CASE WHEN p.rangoFinal = 1 THEN 1 ELSE 0 END) as victorias,
    AVG(p.puntajeFinal) as promedioScore,
    SUM(p.preguntaCorrecta) as totalCorrectas,
    SUM(p.preguntaRespuesta) as totalRespondidas,
    ROUND(
        100.0 * SUM(p.preguntaCorrecta) / NULLIF(SUM(p.preguntaRespuesta), 0),
        2
    ) as porcentajePrecision
FROM Usuario u
LEFT JOIN Participantes p ON u.usuarioID = p.FKUsuario
WHERE p.abandono IS NOT NULL OR p.abandono IS NULL
GROUP BY u.usuarioID, u.nickName, u.FotoPerfil;

COMMENT ON VIEW v_EstadisticasUsuario IS 'Estadísticas generales de cada usuario';

-- Vista: Trivias populares
CREATE OR REPLACE VIEW v_TriviasPopulares AS
SELECT 
    t.triviaID,
    t.titulo,
    t.descripcion,
    t.categoria,
    t.dificultad,
    t.preguntasTotales,
    t.vecesJugada,
    COUNT(DISTINCT s.salaID) as salasCreadas,
    u.nickName as creadorNickname,
    u.usuarioID as creadorID
FROM Trivia t
JOIN Usuario u ON t.FKHostUser = u.usuarioID
LEFT JOIN Sala s ON t.triviaID = s.FKTrivia
WHERE t.Status = 'activo'
GROUP BY t.triviaID, t.titulo, t.descripcion, t.categoria, t.dificultad,
         t.preguntasTotales, t.vecesJugada, u.nickName, u.usuarioID
ORDER BY t.vecesJugada DESC;

COMMENT ON VIEW v_TriviasPopulares IS 'Ranking de trivias más jugadas';

-- Vista: Salas activas disponibles
CREATE OR REPLACE VIEW v_SalasActivas AS
SELECT 
    s.salaID,
    s.codigoSala,
    s.nombreSala,
    s.status,
    s.usuariosActuales,
    s.max_usuario,
    s.esPublico,
    s.fechaCreacion,
    t.titulo as tituloTrivia,
    t.categoria as categoriaTrivia,
    t.preguntasTotales,
    u.nickName as hostNickname
FROM Sala s
JOIN Trivia t ON s.FKTrivia = t.triviaID
JOIN Usuario u ON s.FKUsuario = u.usuarioID
WHERE s.status IN ('esperando', 'en_progreso')
  AND s.esPublico = true
ORDER BY s.fechaCreacion DESC;

COMMENT ON VIEW v_SalasActivas IS 'Salas públicas activas disponibles para unirse';

-- ============================================================================
-- MENSAJE FINAL
-- ============================================================================

DO $$
BEGIN
    RAISE NOTICE '✅ Base de datos Sparky Trivia creada exitosamente';
    RAISE NOTICE '📊 Total de tablas creadas: 7';
    RAISE NOTICE '🔍 Total de índices creados: ~30';
    RAISE NOTICE '⚡ Total de funciones creadas: 4';
    RAISE NOTICE '🎯 Total de triggers creados: 4';
    RAISE NOTICE '👁️  Total de vistas creadas: 4';
    RAISE NOTICE '';
    RAISE NOTICE '⭐ CAMBIOS IMPORTANTES respecto a tu diagrama:';
    RAISE NOTICE '   1. Usuario: AGREGADO passwordHash (CRÍTICO para login)';
    RAISE NOTICE '   2. Preguntas: AGREGADO puntos (CRÍTICO para scoring)';
    RAISE NOTICE '   3. OpcionesRespuesta: AGREGADO textoOpcion (CRÍTICO)';
    RAISE NOTICE '   4. Trivia: AGREGADO esPublico (recomendado)';
    RAISE NOTICE '   5. Trivia: jugadoresActuales → vecesJugada (claridad)';
    RAISE NOTICE '   6. Participantes: nickname-Juego → nicknameJuego (sintaxis)';
    RAISE NOTICE '   7. Participantes: peguntaCorrecta → preguntaCorrecta (typo)';
END $$;