/**
 * Esta parte maneja toda la autenticacion de las sesiones de usaurios 
 *
* */



// URL base del backend
const API_BASE_URL = '/SparkyTrivia/api';

// Endpoints de autenticación
const AUTH_ENDPOINTS = {
    registro: `${API_BASE_URL}/auth/registro`,    // POST - Registrar usuario
    login: `${API_BASE_URL}/auth/login`,          // POST - Iniciar sesión
    logout: `${API_BASE_URL}/auth/logout`         // POST - Cerrar sesión
};

// aqui se guardan los datos de usuario
const STORAGE_KEYS = {
    usuario: 'sparky_usuario',           // Datos del usuario
    token: 'sparky_token',
    sesionActiva: 'sparky_sesion_activa' // Bandera que indica si hay sesión
};

/*
   FUNCIONES DE ALMACENAMIENTO LOCAL
   Estas funciones guardan y recuperan datos del navegador */
function guardarUsuario(usuario) {
    try {
        // Convertir el objeto JavaScript a texto JSON
        localStorage.setItem(STORAGE_KEYS.usuario, JSON.stringify(usuario));
        // Marcar que hay una sesión activa
        localStorage.setItem(STORAGE_KEYS.sesionActiva, 'true');

        console.log('Usuario guardado en localStorage:', usuario);
    } catch (error) {
        console.error('Error al guardar usuario:', error);
    }
}

/**
 * Obtener datos del usuario de localStorage
 */
function obtenerUsuario() {
    try {
        // Obtener el texto JSON guardado
        const usuarioJSON = localStorage.getItem(STORAGE_KEYS.usuario);
        // Si no hay nada guardado, retornar null
        if (!usuarioJSON) {
            return null;
        }

        // Convertir el texto JSON de vuelta a objeto JavaScript
        return JSON.parse(usuarioJSON);
    } catch (error) {
        console.error('Error al obtener usuario:', error);
        return null;
    }
}


//se limpian todos los dagtos de la sesion del localStorage en el logout
function limpiarSesion() {
    try {
        // Eliminar cada dato guardado
        localStorage.removeItem(STORAGE_KEYS.usuario);
        localStorage.removeItem(STORAGE_KEYS.token);
        localStorage.removeItem(STORAGE_KEYS.sesionActiva);

        console.log('Sesión limpiada del localStorage');
    } catch (error) {
        console.error('Error al limpiar sesión:', error);
    }
}


function haySesionActiva() {
    // Verificar la bandera de sesión activa
    const sesionActiva = localStorage.getItem(STORAGE_KEYS.sesionActiva);

    // Verificar que existan datos de usuario
    const usuario = obtenerUsuario();

    // Solo retornar true si AMBAS cosas existen
    return sesionActiva === 'true' && usuario !== null;
}

/*
   PROTECCIÓN DE PÁGINAS
   Estas funciones evitan que usuarios no logueados
   accedan a páginas protegidas
   si no hay sesion redirige al login*/

function protegerPagina(redirectUrl = 'login.html') {
    // Verificar si hay sesión activa
    if (!haySesionActiva()) {
        console.log('No hay sesión activa. Redirigiendo a login...');
        // Redirigir al login
        window.location.href = redirectUrl;
    }
}

//si ya hay sesion , redirige al dash board
function verificarSesionExistente(redirectUrl = 'dashboard.html') {
    // Si ya hay sesión, redirigir al dashboard
    if (haySesionActiva()) {
        console.log('Sesión activa detectada. Redirigiendo a dashboard...');
        window.location.href = redirectUrl;
    }
}

/*
   FUNCIONES DE PETICIONES HTTP
   Estas funciones se comunican con el backend
   realiza las peticiones POST*/

async function postRequest(url, data) {
    try {
        console.log('Enviando POST a:', url);
        console.log('Datos:', data);

        // fetch es la función de JavaScript para hacer peticiones HTTP
        const response = await fetch(url, {
            method: 'POST',                              // Método HTTP
            headers: {
                'Content-Type': 'application/json'       // Tipo de datos (JSON)
            },
            body: JSON.stringify(data),                  // Convertir datos a JSON
            credentials: 'include'                        // Incluir cookies (para sesiones)
        });

        // Convertir la respuesta a JSON
        const jsonResponse = await response.json();

        console.log('Respuesta del servidor:', jsonResponse);

        return jsonResponse;
    } catch (error) {
        console.error('Error en petición POST:', error);
        throw error; // Lanzar el error para que lo maneje quien llamó esta función
    }
}

//peticiones GET
async function getRequest(url) {
    try {
        console.log('Enviando GET a:', url);

        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'                        // Incluir cookies
        });

        const jsonResponse = await response.json();

        console.log('Respuesta del servidor:', jsonResponse);

        return jsonResponse;
    } catch (error) {
        console.error('Error en petición GET:', error);
        throw error;
    }
}

/*
   FUNCIONES DE AUTENTICACIÓN
   cierra sesion, */

async function cerrarSesion() {
    try {
        console.log('Cerrando sesión...');

        // Hacer petición al backend para cerrar sesión en el servidor
        const response = await postRequest(AUTH_ENDPOINTS.logout, {});

        // Limpiar datos locales del navegador
        limpiarSesion();

        console.log('Sesión cerrada exitosamente');

        // Redirigir al login
        window.location.href = 'login.html';

        return true;
    } catch (error) {
        console.error('Error al cerrar sesión:', error);

        // Aunque haya error, limpiar datos locales de todas formas
        limpiarSesion();

        // Redirigir al login de todas formas
        window.location.href = 'login.html';

        return false;
    }
}


function obtenerNickname() {
    const usuario = obtenerUsuario();
    return usuario ? usuario.nickName : null;
}

function obtenerEmail() {
    const usuario = obtenerUsuario();
    return usuario ? usuario.email : null;
}

function obtenerUsuarioId() {
    const usuario = obtenerUsuario();
    return usuario ? usuario.usuarioId : null;
}

/*
   VALIDACIONES COMUNES
   Funciones para validar datos de formularios
    */
function validarEmail(email) {
    // Expresión regular (regex) para validar email
    // Busca: texto @ texto . texto
    const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return regex.test(email);
}

function validarFortalezaPassword(password) {
    let score = 0;
    let texto = '';

    // Criterios de fortaleza (cada uno suma puntos)
    if (password.length >= 6) score += 20;           // Longitud mínima
    if (password.length >= 10) score += 20;          // Longitud buena
    if (/[a-z]/.test(password)) score += 20;         // Tiene minúsculas
    if (/[A-Z]/.test(password)) score += 20;         // Tiene mayúsculas
    if (/[0-9]/.test(password)) score += 10;         // Tiene números
    if (/[^a-zA-Z0-9]/.test(password)) score += 10;  // Tiene caracteres especiales

    // Determinar texto según el puntaje
    if (score === 0) {
        texto = '';
    } else if (score <= 40) {
        texto = 'Débil';       // Rojo
    } else if (score <= 70) {
        texto = 'Media';       // Amarillo
    } else {
        texto = 'Fuerte';      // Verde
    }

    return { score, texto };
}


function validarNickname(nickname) {
    // Expresión regular: solo alfanuméricos, guiones y guiones bajos
    const regex = /^[a-zA-Z0-9_-]+$/;
    return regex.test(nickname) && nickname.length >= 3;
}

function validarPasswordsCoinciden(password1, password2) {
    return password1 === password2 && password1.length > 0;
}

/*
   UTILIDADES DE UI
   Funciones para manipular la interfaz
    */

function mostrarError(elemento, mensaje) {
    elemento.textContent = mensaje;
    elemento.classList.remove('d-none');
}

function ocultarError(elemento) {
    elemento.classList.add('d-none');
}

function mostrarAlerta(mensaje, tipo = 'success', containerId = 'alertContainer') {
    const container = document.getElementById(containerId);
    if (!container) return;

    // Crear elemento de alerta
    const alerta = document.createElement('div');
    alerta.className = `alert alert-${tipo} animate-fadeInDown`;
    alerta.innerHTML = `
        <i class="bi bi-${tipo === 'success' ? 'check-circle-fill' : 'exclamation-triangle-fill'} me-2"></i>
        <span>${mensaje}</span>
    `;

    // Agregar al contenedor
    container.innerHTML = '';
    container.appendChild(alerta);

    // Auto-ocultar después de 5 segundos
    setTimeout(() => {
        alerta.classList.add('animate-fadeOut');
        setTimeout(() => alerta.remove(), 500);
    }, 5000);
}

function formatearFecha(fechaISO) {
    try {
        // Crear objeto Date
        const fecha = new Date(fechaISO);

        // Opciones de formato
        const opciones = {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };

        // Formatear fecha en español
        return fecha.toLocaleDateString('es-ES', opciones);
    } catch (error) {
        console.error('Error al formatear fecha:', error);
        return fechaISO;
    }
}

function escaparHTML(texto) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return texto.replace(/[&<>"']/g, m => map[m]);
}


function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/*
   INICIALIZACIÓN
   Este código se ejecuta cuando se carga el archivo
   */

// Log de carga del archivo
console.log('auth.js cargado correctamente');

// Verificar si estamos en una página que requiere autenticación
// (Esto se puede personalizar según tus necesidades)
document.addEventListener('DOMContentLoaded', function() {
    // Obtener el nombre del archivo HTML actual
    const pagina = window.location.pathname.split('/').pop();

    // Páginas protegidas (requieren login)
    const paginasProtegidas = [
        'dashboard.html',
        'mis-trivias.html',
        'crear-trivia.html',
        'editar-trivia.html',
        'crear-sala.html',
        'lobby.html',
        'juego.html'
    ];

    // Si estamos en una página protegida, verificar sesión
    if (paginasProtegidas.includes(pagina)) {
        protegerPagina();
    }

    // Páginas de autenticación (si ya hay sesión, redirigir)
    const paginasAuth = ['login.html', 'registro.html'];
    if (paginasAuth.includes(pagina)) {
        verificarSesionExistente();
    }
});
