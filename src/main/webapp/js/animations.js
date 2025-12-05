
// ESTAS SON LSA LIBRERIAS PARA MEJORAR LOS ESTILOS QUE USAREMOS
// - GSAP (animaciones avanzadas)   Fuente: https://greensock.com/gsap/
// - Canvas Confetti (papelitos)    Fuente: https://www.npmjs.com/package/canvas-confetti
// - Typed.js (texto escribiéndose)   Fuente: https://github.com/mattboldt/typed.js
// - Vanilla Tilt (efecto 3D)
// - AOS (animaciones al scroll)
// - Particles.js (partículas de fondo)


/**
 * Función principal que inicializa TODAS las animaciones
 * Se ejecuta automáticamente cuando la página está lista
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('Iniciando sistema de animaciones de Sparky Trivia...');

    // Inicializar cada sistema de animación
    initAOS();                    // Animaciones al hacer scroll
    initSparkyAnimations();       // Animaciones de la mascota Sparky
    initHoverEffects();           // Efectos hover en botones
    initParticles();              // Partículas en el fondo
    initTilt();                   // Efecto 3D en cards
    initHeaderScroll();           // Efecto del header al scroll

    console.log('Sistema de animaciones cargado exitosamente');
});

/**
 * Inicializar animaciones automáticas de Sparky
 * Hace que Sparky "respire" y "flote" constantemente
 */
function initSparkyAnimations() {
    // Verificar que GSAP esté cargado
    if (typeof gsap === 'undefined') {
        console.warn('GSAP no está cargado. Las animaciones de Sparky no funcionarán.');
        return;
    }

    console.log('Iniciando animaciones de Sparky...');

    // Seleccionar TODAS las imágenes de Sparky en la página
    const sparkyImages = document.querySelectorAll('.sparky-container img, .sparky-avatar, #sparky');

    // Aplicar animaciones a cada imagen de Sparky encontrada
    sparkyImages.forEach(sparky => {
        // ANIMACIÓN 1: Respiración
        gsap.to(sparky, {
            scale: 1.05,              // Crecer al 105% de su tamaño
            duration: 2,              // Duración de 2 segundos
            repeat: -1,               // Repetir infinitamente (-1 = infinito)
            yoyo: true,               // Ir y volver (crecer y achicar)
            ease: "power1.inOut"      // Suavizado de la animación
        });

        // ANIMACIÓN 2: Flotación (movimiento vertical)
        gsap.to(sparky, {
            y: -15,                   // Mover 15 píxeles hacia arriba
            duration: 3,              // Duración de 3 segundos
            repeat: -1,               // Repetir infinitamente
            yoyo: true,               // Ir y volver
            ease: "sine.inOut"        // Suavizado sinusoidal (muy suave)
        });

        // ANIMACIÓN 3: Rotación sutil
        gsap.to(sparky, {
            rotation: 3,              // Rotar 3 grados
            duration: 4,              // Duración de 4 segundos
            repeat: -1,               // Repetir infinitamente
            yoyo: true,               // Ir y volver
            ease: "sine.inOut"
        });
    });

    console.log(`Animaciones aplicadas a ${sparkyImages.length} imagen(es) de Sparky`);
}

/**
 * Animación cuando ACIERTAS una respuesta
 * Sparky salta feliz, gira y lanza confetti
 */
function sparkyHappyAnimation(element) {
    // Verificar que GSAP esté disponible
    if (typeof gsap === 'undefined') return;
    console.log('¡Sparky está feliz!');
    // Timeline de GSAP (secuencia de animaciones)
    const tl = gsap.timeline();
    // Secuencia de animaciones:
    // Crecer y girar
    tl.to(element, {
        scale: 1.3,               // Crecer al 130%
        rotation: 360,            // Girar 360 grados (una vuelta completa)
        duration: 0.5,            // En medio segundo
        ease: "back.out(2)"       // Efecto de rebote al final
    })
        // 2. Volver al tamaño normal
        .to(element, {
            scale: 1,                 // Volver al tamaño original (100%)
            rotation: 0,              // Volver a rotación 0
            duration: 0.3             // En 0.3 segundos
        });

    // Cambiar temporalmente a imagen feliz
    const originalSrc = element.src;
    element.src = element.src.replace(/sparky_\w+\.png/, 'sparky_feliz.png');
    // Volver a la imagen original después de 2 segundos
    setTimeout(() => {
        element.src = originalSrc;
    }, 2000);
    // Lanzar confetti
    launchConfetti();
}

/**
 * Animación cuando FALLAS una respuesta
 * Sparky se sacude y se pone triste
 */
function sparkySadAnimation(element) {
    if (typeof gsap === 'undefined') return;
    console.log('Sparky está triste...');
    // Animación de sacudida horizontal (shake)
    gsap.to(element, {
        x: -10,                   // Mover 10px a la izquierda
        duration: 0.1,            // Muy rápido
        yoyo: true,               // Ir y volver
        repeat: 5,                // Repetir 5 veces (sacude 5 veces)
        ease: "power1.inOut"
    });

    // Cambiar a imagen triste
    const originalSrc = element.src;
    element.src = element.src.replace(/sparky_\w+\.png/, 'sparky_triste.png');
    // Volver a la imagen original después de 2 segundos
    setTimeout(() => {
        element.src = originalSrc;
    }, 2000);
}

/**
 * Animación cuando Sparky está PENSANDO
 * Inclina la cabeza y cambia a imagen pensando
 */
function sparkyThinkingAnimation(element) {
    if (typeof gsap === 'undefined') return;
    console.log('Sparky está pensando...');
    // Cambiar a imagen pensando
    const originalSrc = element.src;
    element.src = element.src.replace(/sparky_\w+\.png/, 'sparky_pensando.png');
    // Animación de inclinación de cabeza
    gsap.to(element, {
        rotation: 5,              // Inclinar 5 grados
        duration: 0.5,
        yoyo: true,
        repeat: -1,               // Infinito mientras "piensa"
        ease: "sine.inOut"
    });
}

/**
 * Lanzar confetti simple
 * Se usa cuando aciertas una pregunta
 * Usa Canvas Confetti
 */
function launchConfetti() {
    // Verificar que la librería esté cargada
    if (typeof confetti === 'undefined') {
        console.warn('Canvas Confetti no está cargado');
        return;
    }

    console.log('Lanzando confetti...');

    // Configuración del confetti
    confetti({
        particleCount: 100,       // Cantidad de papelitos
        spread: 70,               // Qué tan dispersos (ángulo)
        origin: { y: 0.6 },       // Desde dónde sale (60% de altura)
        colors: ['#667eea', '#764ba2', '#2dce89', '#ffd600', '#f5365c']  // Colores de Sparky
    });
}

/**
 * Explosión de confetti (victoria grande)
 * Se usa cuando GANAS la trivia
 */
function confettiExplosion() {
    if (typeof confetti === 'undefined') return;

    console.log('¡EXPLOSIÓN DE CONFETTI!');

    // Duración de la explosión
    const duration = 3 * 1000;  // 3 segundos
    const animationEnd = Date.now() + duration;

    // Configuración base
    const defaults = {
        startVelocity: 30,        // Velocidad inicial
        spread: 360,              // 360 grados (todas direcciones)
        ticks: 60,                // Duración de cada papelito
        zIndex: 0
    };

    // Función para generar número aleatorio
    function randomInRange(min, max) {
        return Math.random() * (max - min) + min;
    }

    // Intervalo que lanza confetti continuamente
    const interval = setInterval(function() {
        const timeLeft = animationEnd - Date.now();

        // Si terminó el tiempo, detener
        if (timeLeft <= 0) {
            return clearInterval(interval);
        }

        // Cantidad de partículas proporcional al tiempo restante
        const particleCount = 50 * (timeLeft / duration);

        // Lanzar desde la IZQUIERDA
        confetti({
            ...defaults,
            particleCount,
            origin: {
                x: randomInRange(0.1, 0.3),  // Entre 10% y 30% desde la izquierda
                y: Math.random() - 0.2
            }
        });

        // Lanzar desde la DERECHA
        confetti({
            ...defaults,
            particleCount,
            origin: {
                x: randomInRange(0.7, 0.9),  // Entre 70% y 90% desde la izquierda
                y: Math.random() - 0.2
            }
        });
    }, 250);  // Cada 250ms (4 veces por segundo)
}

/**
 * Confetti estilo fuegos artificiales
 * Lanzamiento desde abajo hacia arriba
 */
function confettiFireworks() {
    if (typeof confetti === 'undefined') return;

    console.log('Confetti fuegos artificiales');

    const count = 200;
    const defaults = {
        origin: { y: 0.7 }  // Desde 70% de altura
    };

    // Función helper para lanzar confetti
    function fire(particleRatio, opts) {
        confetti({
            ...defaults,
            ...opts,
            particleCount: Math.floor(count * particleRatio)
        });
    }

    // Lanzar en diferentes direcciones y velocidades
    fire(0.25, { spread: 26, startVelocity: 55 });
    fire(0.2, { spread: 60 });
    fire(0.35, { spread: 100, decay: 0.91, scalar: 0.8 });
    fire(0.1, { spread: 120, startVelocity: 25, decay: 0.92, scalar: 1.2 });
    fire(0.1, { spread: 120, startVelocity: 45 });
}

/*
   TEXTO ESCRIBIÉNDOSE (TYPED.JS)*/

/**
 * Inicializar texto que se escribe solo
 *
 * @param {string} elementId - ID del elemento HTML donde se escribirá
 * @param {Array<string>} strings - Array de textos a escribir
 * @returns {Object} - Instancia de Typed
 */
function initTypedText(elementId, strings) {
    // Verificar que Typed.js esté cargado
    if (typeof Typed === 'undefined') {
        console.warn('Typed.js no está cargado');
        return null;
    }

    console.log(`⌨Iniciando texto animado en #${elementId}`);

    // Crear instancia de Typed
    const typed = new Typed(`#${elementId}`, {
        strings: strings,         // Textos a escribir
        typeSpeed: 50,            // Velocidad de escritura (ms por caracter)
        backSpeed: 30,            // Velocidad de borrado
        backDelay: 1500,          // Espera antes de borrar (ms)
        loop: true,               // Repetir infinitamente
        showCursor: true,         // Mostrar cursor parpadeante
        cursorChar: '|',          // Caracter del cursor
    });

    return typed;
}

/**
 * Texto de bienvenida animado para Sparky
 * Uso: En index.html poner: <span id="typed-welcome"></span>
 */
function sparkyTypedWelcome() {
    const messages = [
        '¡Bienvenido a Sparky Trivia! 🧠',
        '¿Listo para demostrar tu conocimiento? 💡',
        '¡Prepárate para la mejor trivia! 🎮',
        '¡Desafía a tus amigos! 🏆'
    ];

    return initTypedText('typed-welcome', messages);
}

/* ==========================================
   5. EFECTOS 3D CON VANILLA TILT
   Fuente: https://micku7zu.github.io/vanilla-tilt.js/
   ========================================== */

/**
 * Inicializar efecto Tilt 3D en cards
 * Las cards se inclinan siguiendo el mouse
 */
function initTilt() {
    // Verificar que VanillaTilt esté cargado
    if (typeof VanillaTilt === 'undefined') {
        console.warn('Vanilla Tilt no está cargado');
        return;
    }

    console.log('Iniciando efectos Tilt 3D...');

    // Seleccionar elementos con clase .card-tilt o .trivia-card, excluyendo login y register cards
    const tiltElements = document.querySelectorAll('.card-tilt:not(.login-card):not(.register-card), .trivia-card, .btn-tilt');

    if (tiltElements.length === 0) {
        console.log('ℹNo se encontraron elementos para Tilt');
        return;
    }

    // Aplicar efecto Tilt a cada elemento
    VanillaTilt.init(tiltElements, {
        max: 15,                  // Inclinación máxima (grados)
        speed: 400,               // Velocidad de la transición (ms)
        glare: true,              // Efecto de brillo
        "max-glare": 0.3,         // Opacidad máxima del brillo
        scale: 1.05,              // Escala al hacer hover (105%)
        perspective: 1000,        // Perspectiva 3D
    });

    console.log(`Tilt aplicado a ${tiltElements.length} elemento(s)`);
}

/**
 * Aplicar Tilt a un elemento específico
 * @param {HTMLElement} element - Elemento al que aplicar Tilt
 */
function applyTilt(element) {
    if (typeof VanillaTilt === 'undefined') return;

    VanillaTilt.init(element, {
        max: 10,
        speed: 300,
        glare: true,
        "max-glare": 0.2,
    });
}

/* ==========================================
   6. ANIMACIONES AL HACER SCROLL (AOS)
   Fuente: https://michalsnik.github.io/aos/
   ========================================== */

/**
 * Inicializar AOS (Animate On Scroll)
 * Los elementos con atributo data-aos se animan al aparecer
 */
function initAOS() {
    // Verificar que AOS esté cargado
    if (typeof AOS === 'undefined') {
        console.warn('AOS no está cargado');
        return;
    }

    console.log('Iniciando animaciones al scroll (AOS)...');

    // Inicializar AOS con configuración
    AOS.init({
        duration: 800,            // Duración de animaciones (ms)
        easing: 'ease-in-out',    // Tipo de suavizado
        once: true,               // Animar solo una vez (no cada vez que aparece)
        offset: 100,              // Offset desde el punto de disparo (px)
        delay: 0,                 // Delay de animación (ms)
        mirror: false,            // No animar al volver hacia arriba
    });

    console.log('AOS inicializado');
}

/**
 * Refrescar AOS (útil cuando agregas contenido dinámicamente)
 */
function refreshAOS() {
    if (typeof AOS !== 'undefined') {
        AOS.refresh();
        console.log('AOS refrescado');
    }
}

/* ==========================================
   7. EFECTOS DE HOVER EN BOTONES
   ========================================== */

/**
 * Inicializar efectos de hover y ripple en botones
 */
function initHoverEffects() {
    console.log('Iniciando efectos hover...');

    // Seleccionar todos los botones con clase .btn o .btn-ripple
    const buttons = document.querySelectorAll('.btn-ripple, .btn');

    // Agregar listener de click a cada botón
    buttons.forEach(button => {
        button.addEventListener('click', createRipple);
    });

    console.log(`Efectos hover aplicados a ${buttons.length} botón(es)`);
}

/**
 * Crear efecto ripple (onda) en un botón al hacer click
 * Fuente: Material Design
 *
 * @param {Event} event - Evento de click
 */
function createRipple(event) {
    const button = event.currentTarget;

    // Crear elemento span para el ripple
    const ripple = document.createElement('span');
    const diameter = Math.max(button.clientWidth, button.clientHeight);
    const radius = diameter / 2;

    // Posicionar el ripple donde se hizo click
    ripple.style.width = ripple.style.height = `${diameter}px`;
    ripple.style.left = `${event.clientX - button.offsetLeft - radius}px`;
    ripple.style.top = `${event.clientY - button.offsetTop - radius}px`;
    ripple.classList.add('ripple-effect');

    // Eliminar ripple anterior si existe
    const oldRipple = button.querySelector('.ripple-effect');
    if (oldRipple) {
        oldRipple.remove();
    }

    // Agregar ripple al botón
    button.appendChild(ripple);

    // Eliminar después de la animación (600ms)
    setTimeout(() => {
        ripple.remove();
    }, 600);
}

/* ==========================================
   8. PARTÍCULAS EN EL FONDO
   Fuente: https://vincentgarreau.com/particles.js/
   ========================================== */

/**
 * Inicializar partículas animadas en el fondo
 * Requiere un elemento <div id="particles-js"></div> en el HTML
 */
function initParticles() {
    // Verificar que particlesJS esté cargado
    if (typeof particlesJS === 'undefined') {
        console.warn('Particles.js no está cargado');
        return;
    }

    // Verificar que exista el elemento en el HTML
    if (!document.getElementById('particles-js')) {
        console.log('ℹNo se encontró #particles-js en esta página');
        return;
    }

    console.log('Iniciando partículas de fondo...');

    // Configuración de las partículas
    particlesJS('particles-js', {
        particles: {
            number: {
                value: 80,            // Cantidad de partículas
                density: {
                    enable: true,
                    value_area: 800   // Densidad por área
                }
            },
            color: {
                // Colores de Sparky Trivia
                value: ['#667eea', '#764ba2', '#2dce89', '#ffd600']
            },
            shape: {
                type: 'circle',       // Forma de las partículas
                stroke: {
                    width: 0,
                    color: '#000000'
                }
            },
            opacity: {
                value: 0.5,           // Opacidad
                random: true,         // Opacidad aleatoria
                anim: {
                    enable: true,
                    speed: 1,
                    opacity_min: 0.1,
                    sync: false
                }
            },
            size: {
                value: 3,             // Tamaño de partículas
                random: true,
                anim: {
                    enable: true,
                    speed: 2,
                    size_min: 0.1,
                    sync: false
                }
            },
            line_linked: {
                enable: true,         // Líneas que conectan partículas
                distance: 150,        // Distancia máxima de conexión
                color: '#667eea',     // Color de líneas
                opacity: 0.4,
                width: 1
            },
            move: {
                enable: true,         // Las partículas se mueven
                speed: 2,             // Velocidad de movimiento
                direction: 'none',    // Sin dirección específica
                random: false,
                straight: false,
                out_mode: 'out',      // Salen y vuelven a entrar
                bounce: false,
            }
        },
        interactivity: {
            detect_on: 'canvas',
            events: {
                onhover: {
                    enable: true,
                    mode: 'repulse'   // Se alejan del cursor
                },
                onclick: {
                    enable: true,
                    mode: 'push'      // Agregar partículas al hacer click
                },
                resize: true
            },
            modes: {
                repulse: {
                    distance: 100,
                    duration: 0.4
                },
                push: {
                    particles_nb: 4   // Cantidad de partículas al hacer click
                }
            }
        },
        retina_detect: true           // Soporte para pantallas retina
    });

    console.log('Partículas inicializadas');
}

/* ==========================================
   9. HEADER CON EFECTO AL SCROLL
   ========================================== */

/**
 * Inicializar efecto del header al hacer scroll
 * El header se vuelve más opaco cuando scrolleas
 */
function initHeaderScroll() {
    const header = document.querySelector('.header');

    // Si no hay header, salir
    if (!header) {
        console.log('ℹNo se encontró .header en esta página');
        return;
    }

    console.log('Iniciando efecto de scroll en header...');

    // Listener de scroll
    window.addEventListener('scroll', function() {
        // Si se ha scrolleado más de 50px
        if (window.scrollY > 50) {
            header.classList.add('scrolled');  // Agregar clase
        } else {
            header.classList.remove('scrolled');  // Quitar clase
        }
    });

    console.log('Efecto de header inicializado');
}

/* ==========================================
   10. UTILIDADES DE ANIMACIÓN
   ========================================== */

/**
 * Animar entrada de un elemento con una animación específica
 *
 * @param {HTMLElement} element - Elemento a animar
 * @param {string} animation - Tipo de animación (fadeIn, slideUp, bounceIn, etc)
 */
function animateIn(element, animation = 'fadeIn') {
    element.classList.add(`animate-${animation}`);

    // Eliminar clase después de que termine la animación
    element.addEventListener('animationend', () => {
        element.classList.remove(`animate-${animation}`);
    }, { once: true });  // Solo una vez
}

/**
 * Hacer shake (sacudir) a un elemento
 * Útil para mostrar errores
 *
 * @param {HTMLElement} element - Elemento a sacudir
 */
function shakeElement(element) {
    element.classList.add('animate-shake');

    // Quitar clase después de 500ms
    setTimeout(() => {
        element.classList.remove('animate-shake');
    }, 500);
}

/**
 * Hacer pulse (pulso) a un elemento
 * Útil para llamar la atención
 *
 * @param {HTMLElement} element - Elemento a animar
 */
function pulseElement(element) {
    element.classList.add('animate-pulse');

    setTimeout(() => {
        element.classList.remove('animate-pulse');
    }, 2000);
}

/**
 * Transición suave entre páginas
 * Hace fade out antes de cambiar de página
 *
 * @param {string} url - URL a la que navegar
 */
function smoothPageTransition(url) {
    // Fade out del body
    document.body.style.opacity = '0';
    document.body.style.transition = 'opacity 0.3s ease';

    // Navegar después del fade
    setTimeout(() => {
        window.location.href = url;
    }, 300);
}

/* ==========================================
   11. OBJETO GLOBAL DE ANIMACIONES
   Agrupa todas las funciones para fácil acceso
   ========================================== */

// Crear objeto global SparkyAnimations
window.SparkyAnimations = {
    // Sparky (mascota)
    sparkyHappy: sparkyHappyAnimation,
    sparkySad: sparkySadAnimation,
    sparkyThinking: sparkyThinkingAnimation,

    // Confetti
    confetti: launchConfetti,
    confettiExplosion: confettiExplosion,
    confettiFireworks: confettiFireworks,

    // Texto
    typedText: initTypedText,
    sparkyWelcome: sparkyTypedWelcome,

    // 3D
    tilt: applyTilt,

    // Utilidades
    animateIn: animateIn,
    shake: shakeElement,
    pulse: pulseElement,
    pageTransition: smoothPageTransition,
    refreshAOS: refreshAOS
};

console.log('Objeto SparkyAnimations disponible globalmente');
console.log('Ejemplo de uso: SparkyAnimations.confetti()');
