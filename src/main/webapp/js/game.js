
// Variables globales del juego
let websocket = null;
let codigoSala = null;
let usuario = null;
let participanteId = null;
let preguntaActual = null;
let tiempoInicio = null;
let timerInterval = null;
let puedeResponder = true;

// Configuración
const WS_RECONNECT_DELAY = 3000; // 3 segundos
const HEARTBEAT_INTERVAL = 30000; // 30 segundos


function iniciarConexionWebSocket(codigo, user, participante) {
    codigoSala = codigo;
    usuario = user;
    participanteId = participante;

    console.log(' Iniciando conexión WebSocket para juego...');
    console.log('Código sala:', codigoSala);
    console.log('Usuario:', usuario.nickName);
    console.log('Participante ID:', participanteId);

    conectarWebSocket();

    // Heartbeat para mantener conexión viva
    setInterval(() => {
        if (websocket && websocket.readyState === WebSocket.OPEN) {
            enviarMensaje({ tipo: 'PING' });
        }
    }, HEARTBEAT_INTERVAL);
}


function conectarWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/SparkyTrivia/game/${codigoSala}`;

    console.log(' Conectando a:', wsUrl);

    try {
        websocket = new WebSocket(wsUrl);

        websocket.onopen = onWebSocketOpen;
        websocket.onmessage = onWebSocketMessage;
        websocket.onerror = onWebSocketError;
        websocket.onclose = onWebSocketClose;

    } catch (error) {
        console.error(' Error creando WebSocket:', error);
        mostrarError('Error de conexión. Intentando reconectar...');
        setTimeout(conectarWebSocket, WS_RECONNECT_DELAY);
    }
}

/**
 * EVENTOS DEL WEBSOCKET
 */
function onWebSocketOpen(event) {
    console.log(' WebSocket conectado');
    actualizarEstadoConexion(true);

    // Notificar que estoy listo para jugar
    enviarMensaje({
        tipo: 'UNIRSE',
        usuarioId: usuario.usuarioId,
        nickname: usuario.nickName,
        participanteId: participanteId
    });
}

function onWebSocketMessage(event) {
    console.log(' Mensaje recibido:', event.data);

    try {
        const mensaje = JSON.parse(event.data);

        switch (mensaje.tipo) {
            case 'CONECTADO':
                console.log(' Confirmación de conexión');
                break;

            case 'PREGUNTA':
                manejarNuevaPregunta(mensaje);
                break;

            case 'RESPUESTA_CORRECTA':
                manejarRespuestaCorrecta(mensaje);
                break;

            case 'RANKING':
                actualizarRanking(mensaje.ranking);
                break;

            case 'JUEGO_FINALIZADO':
                manejarFinJuego(mensaje);
                break;

            case 'PONG':
                console.log(' Heartbeat recibido');
                break;

            case 'ERROR':
                console.error(' Error del servidor:', mensaje.mensaje);
                mostrarError(mensaje.mensaje);
                break;

            default:
                console.log(' Tipo de mensaje desconocido:', mensaje.tipo);
        }

    } catch (error) {
        console.error(' Error parseando mensaje:', error);
    }
}

function onWebSocketError(error) {
    console.error(' Error WebSocket:', error);
    actualizarEstadoConexion(false);
    mostrarError('Error de conexión con el servidor');
}

function onWebSocketClose(event) {
    console.log(' WebSocket desconectado');
    actualizarEstadoConexion(false);

    // Intentar reconectar si no fue cierre intencional
    if (event.code !== 1000) {
        console.log(' Intentando reconectar en', WS_RECONNECT_DELAY / 1000, 'segundos...');
        setTimeout(conectarWebSocket, WS_RECONNECT_DELAY);
    }
}


function manejarNuevaPregunta(mensaje) {
    console.log(' Nueva pregunta recibida');

    preguntaActual = mensaje.pregunta;
    puedeResponder = true;
    tiempoInicio = Date.now();

    // Actualizar UI
    mostrarPregunta(mensaje);

    // Iniciar temporizador
    iniciarTemporizador(preguntaActual.limiteTiempo);
}


function mostrarPregunta(mensaje) {
    // Actualizar número de pregunta
    document.getElementById('numeroPregunta').textContent =
        `Pregunta ${mensaje.numeroPregunta} de ${mensaje.totalPreguntas}`;

    // Mostrar contenido de la pregunta
    document.getElementById('contenidoPregunta').textContent = preguntaActual.contenido;

    // Mostrar imagen si existe
    const imagenContainer = document.getElementById('imagenPregunta');
    if (preguntaActual.imagen) {
        imagenContainer.innerHTML = `<img src="${preguntaActual.imagen}" alt="Imagen" class="img-fluid rounded">`;
        imagenContainer.classList.remove('d-none');
    } else {
        imagenContainer.classList.add('d-none');
    }

    // Mostrar opciones
    const opcionesContainer = document.getElementById('opcionesContainer');
    opcionesContainer.innerHTML = '';

    preguntaActual.opciones.forEach((opcion, index) => {
        const btn = document.createElement('button');
        btn.className = 'btn btn-opcion';
        btn.innerHTML = `
            <span class="opcion-letra">${String.fromCharCode(65 + index)}</span>
            <span class="opcion-texto">${opcion.textoOpcion}</span>
        `;
        btn.onclick = () => seleccionarOpcion(opcion.opcionId, btn);
        opcionesContainer.appendChild(btn);
    });

    // Mostrar puntos
    document.getElementById('puntosPreg').textContent = preguntaActual.puntos;
}


function iniciarTemporizador(segundos) {
    let tiempoRestante = segundos;

    const progressBar = document.getElementById('timerProgress');
    const timerText = document.getElementById('timerText');

    actualizarTimer(tiempoRestante, segundos);

    clearInterval(timerInterval);
    timerInterval = setInterval(() => {
        tiempoRestante--;

        if (tiempoRestante <= 0) {
            clearInterval(timerInterval);
            tiempoAgotado();
        } else {
            actualizarTimer(tiempoRestante, segundos);
        }
    }, 1000);
}

function actualizarTimer(restante, total) {
    const porcentaje = (restante / total) * 100;
    const progressBar = document.getElementById('timerProgress');
    const timerText = document.getElementById('timerText');

    progressBar.style.width = porcentaje + '%';
    timerText.textContent = restante + 's';

    // Cambiar color según tiempo
    if (porcentaje > 50) {
        progressBar.className = 'progress-bar bg-success';
    } else if (porcentaje > 20) {
        progressBar.className = 'progress-bar bg-warning';
    } else {
        progressBar.className = 'progress-bar bg-danger';
    }
}

function tiempoAgotado() {
    console.log(' Tiempo agotado');
    puedeResponder = false;

    // Deshabilitar botones
    document.querySelectorAll('.btn-opcion').forEach(btn => {
        btn.disabled = true;
    });

    mostrarMensaje(' Tiempo agotado', 'warning');
}

function seleccionarOpcion(opcionId, btnElement) {
    if (!puedeResponder) {
        console.log(' Ya no puedes responder');
        return;
    }

    puedeResponder = false;
    clearInterval(timerInterval);

    // Calcular tiempo tomado
    const tiempoTomado = Math.floor((Date.now() - tiempoInicio) / 1000);

    console.log(' Opción seleccionada:', opcionId);
    console.log(' Tiempo tomado:', tiempoTomado, 'segundos');

    // Marcar opción seleccionada visualmente
    document.querySelectorAll('.btn-opcion').forEach(btn => {
        btn.disabled = true;
    });
    btnElement.classList.add('seleccionada');

    // Enviar respuesta al servidor
    enviarRespuesta(opcionId, tiempoTomado);
}

function enviarRespuesta(opcionId, tiempoTomado) {
    const respuesta = {
        tipo: 'RESPUESTA',
        participanteId: participanteId,
        preguntaId: preguntaActual.preguntaId,
        opcionId: opcionId,
        tiempoTomado: tiempoTomado
    };

    enviarMensaje(respuesta);
    mostrarMensaje('Respuesta enviada', 'success');
}

function manejarRespuestaCorrecta(mensaje) {
    console.log('Respuesta correcta:', mensaje.opcionCorrectaId);

    // Resaltar opción correcta
    document.querySelectorAll('.btn-opcion').forEach(btn => {
        // Aquí necesitarías comparar con el opcionId, esto es simplificado
        // En producción, deberías almacenar el opcionId en un data-attribute
    });

    // Mostrar explicación si existe
    if (mensaje.explicacion) {
        mostrarExplicacion(mensaje.explicacion);
    }
}


function actualizarRanking(ranking) {
    console.log(' Actualizando ranking');

    const rankingContainer = document.getElementById('rankingLive');
    if (!rankingContainer) return;

    rankingContainer.innerHTML = ranking.slice(0, 5).map((jugador, index) => `
        <div class="ranking-item">
            <span class="ranking-pos">#${jugador.posicion}</span>
            <span class="ranking-nick">${jugador.nickname}</span>
            <span class="ranking-pts">${jugador.puntaje} pts</span>
        </div>
    `).join('');
}

/**
 * MANEJAR FIN DEL JUEGO
 */
function manejarFinJuego(mensaje) {
    console.log(' Juego finalizado');

    clearInterval(timerInterval);

    // Mostrar resultados finales
    mostrarResultadosFinales(mensaje.rankingFinal);

    // Opcional: Redirigir a página de resultados después de 5 segundos
    setTimeout(() => {
        window.location.href = `resultados.html?codigo=${codigoSala}`;
    }, 5000);
}

/**
 * FUNCIONES AUXILIARES
 */
function enviarMensaje(mensaje) {
    if (websocket && websocket.readyState === WebSocket.OPEN) {
        websocket.send(JSON.stringify(mensaje));
    } else {
        console.error(' WebSocket no está conectado');
    }
}

function actualizarEstadoConexion(conectado) {
    const statusIndicator = document.getElementById('conexionStatus');
    if (!statusIndicator) return;

    if (conectado) {
        statusIndicator.className = 'conexion-online';
        statusIndicator.innerHTML = '<i class="bi bi-wifi"></i> Conectado';
    } else {
        statusIndicator.className = 'conexion-offline';
        statusIndicator.innerHTML = '<i class="bi bi-wifi-off"></i> Desconectado';
    }
}

function mostrarError(mensaje) {
    // Implementar según tu UI
    console.error('', mensaje);
}

function mostrarMensaje(mensaje, tipo) {
    // Implementar según tu UI
    console.log('', mensaje);
}

function mostrarExplicacion(texto) {
    // Implementar según tu UI
    console.log('', texto);
}

function mostrarResultadosFinales(ranking) {
    // Implementar según tu UI
    console.log(' Resultados finales:', ranking);
}


window.addEventListener('beforeunload', function() {
    if (websocket) {
        websocket.close(1000, 'Usuario salió de la página');
    }
    clearInterval(timerInterval);
});

// Exportar función principal
window.iniciarJuegoWebSocket = iniciarConexionWebSocket;