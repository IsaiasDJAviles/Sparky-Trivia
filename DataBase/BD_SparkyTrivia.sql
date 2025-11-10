CREATE TABLE Usuario (
                         usuarioID               SERIAL PRIMARY KEY,
                         email                   VARCHAR(255) UNIQUE NOT NULL,
                         passwordHash            VARCHAR(255) NOT NULL,
                         firstName               VARCHAR(100) NOT NULL,
                         lastName                VARCHAR(100) NOT NULL,
                         nickName                VARCHAR(50) UNIQUE NOT NULL,
                         DOB                     DATE,
                         FotoPerfil              VARCHAR(500),
                         Rol                     VARCHAR(20) DEFAULT 'player',
                         Status                  VARCHAR(20) DEFAULT 'activo',
                         fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         fechaActualizacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         fechaLogin              TIMESTAMP
);

CREATE TABLE Trivia (
                        triviaID                SERIAL PRIMARY KEY,
                        titulo                  VARCHAR(255) NOT NULL,
                        descripcion             TEXT,
                        FKHostUser              INTEGER NOT NULL REFERENCES Usuario(usuarioID) ON DELETE CASCADE,
                        categoria               VARCHAR(50) DEFAULT 'general',
                        dificultad              VARCHAR(20) DEFAULT 'medio',
                        preguntasTotales        INTEGER DEFAULT 0,
                        tiempoEstimado          INTEGER DEFAULT 15,
                        fotoPortada             VARCHAR(500),
                        esPublico               BOOLEAN DEFAULT TRUE,
                        Status                  VARCHAR(20) DEFAULT 'borrador',
                        fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        fechaActualizacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        vecesJugada             INTEGER DEFAULT 0
);

CREATE TABLE Preguntas (
                           preguntaID              SERIAL PRIMARY KEY,
                           FKTrivia                INTEGER NOT NULL REFERENCES Trivia(triviaID) ON DELETE CASCADE,
                           orderPregunta           INTEGER NOT NULL,
                           contenido               TEXT NOT NULL,
                           tipo                    VARCHAR(20) DEFAULT 'opcion_multiple',
                           puntos                  INTEGER DEFAULT 100,
                           limiteTiempo            INTEGER DEFAULT 30,
                           dificultad              VARCHAR(20) DEFAULT 'medio',
                           imagenPregunta          VARCHAR(500),
                           explicacion             TEXT,
                           fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT orden_unico UNIQUE(FKTrivia, orderPregunta)
);

CREATE TABLE OpcionesRespuesta (
                                   opcionID                SERIAL PRIMARY KEY,
                                   FKPregunta              INTEGER NOT NULL REFERENCES Preguntas(preguntaID) ON DELETE CASCADE,
                                   orderPregunta           INTEGER NOT NULL,
                                   textoOpcion             TEXT NOT NULL,
                                   isCorrecto              BOOLEAN DEFAULT FALSE,

                                   CONSTRAINT orden_opcion_unico UNIQUE(FKPregunta, orderPregunta)
);

CREATE TABLE Sala (
                      salaID                  SERIAL PRIMARY KEY,
                      codigoSala              VARCHAR(10) UNIQUE NOT NULL,
                      nombreSala              VARCHAR(255),
                      FKTrivia                INTEGER NOT NULL REFERENCES Trivia(triviaID) ON DELETE RESTRICT,
                      FKUsuario               INTEGER NOT NULL REFERENCES Usuario(usuarioID) ON DELETE CASCADE,
                      max_usuario             INTEGER DEFAULT 50,
                      usuariosActuales        INTEGER DEFAULT 0,
                      status                  VARCHAR(20) DEFAULT 'esperando',
                      preguntaActual          INTEGER DEFAULT 0,
                      esPublico               BOOLEAN DEFAULT TRUE,
                      unirseDespues           BOOLEAN DEFAULT FALSE,
                      fechaCreacion           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      inicio                  TIMESTAMP,
                      finalizacion            TIMESTAMP
);

CREATE TABLE Participantes (
                               participanteID          SERIAL PRIMARY KEY,
                               FKSala                  INTEGER NOT NULL REFERENCES Sala(salaID) ON DELETE CASCADE,
                               FKUsuario               INTEGER NOT NULL REFERENCES Usuario(usuarioID) ON DELETE CASCADE,
                               nicknameJuego           VARCHAR(50),
                               unio                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               abandono                TIMESTAMP,
                               esActivo                BOOLEAN DEFAULT TRUE,
                               esHost                  BOOLEAN DEFAULT FALSE,
                               puntajeFinal            INTEGER DEFAULT 0,
                               rangoFinal              INTEGER,
                               preguntaRespuesta       INTEGER DEFAULT 0,
                               preguntaCorrecta        INTEGER DEFAULT 0,

                               CONSTRAINT usuario_unico_sala UNIQUE(FKSala, FKUsuario)
);

CREATE TABLE RespuestasJugador (
                                   respuestaID             SERIAL PRIMARY KEY,
                                   FKParticipante          INTEGER NOT NULL REFERENCES Participantes(participanteID) ON DELETE CASCADE,
                                   FKPregunta              INTEGER NOT NULL REFERENCES Preguntas(preguntaID) ON DELETE CASCADE,
                                   FKSala                  INTEGER NOT NULL REFERENCES Sala(salaID) ON DELETE CASCADE,
                                   opcionSeleccionada      INTEGER REFERENCES OpcionesRespuesta(opcionID) ON DELETE SET NULL,
                                   esCorrecta              BOOLEAN,
                                   tiempoTomado            INTEGER,
                                   puntosGanados           INTEGER DEFAULT 0,
                                   respondioEn             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT respuesta_unica UNIQUE(FKParticipante, FKPregunta)
);

-- Usuario
CREATE INDEX idx_usuario_email ON Usuario(email);
CREATE INDEX idx_usuario_nickname ON Usuario(nickName);

-- Trivia
CREATE INDEX idx_trivia_host ON Trivia(FKHostUser);
CREATE INDEX idx_trivia_categoria ON Trivia(categoria);
CREATE INDEX idx_trivia_status ON Trivia(Status);

-- Preguntas
CREATE INDEX idx_preguntas_trivia ON Preguntas(FKTrivia);

-- OpcionesRespuesta
CREATE INDEX idx_opciones_pregunta ON OpcionesRespuesta(FKPregunta);

-- Sala
CREATE INDEX idx_sala_codigo ON Sala(codigoSala);
CREATE INDEX idx_sala_trivia ON Sala(FKTrivia);

-- Participantes
CREATE INDEX idx_participantes_sala ON Participantes(FKSala);
CREATE INDEX idx_participantes_usuario ON Participantes(FKUsuario);

-- RespuestasJugador
CREATE INDEX idx_respuestas_participante ON RespuestasJugador(FKParticipante);
CREATE INDEX idx_respuestas_sala ON RespuestasJugador(FKSala);