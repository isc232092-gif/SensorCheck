package com.example.sensorcheck.domain.diagnostico

import com.example.sensorcheck.domain.diagnostico.CategoriaPrueba as C
import com.example.sensorcheck.domain.diagnostico.IdPrueba as P

/**
 * Catálogo de pruebas físicas (GRASP: Experto en información).
 * Describe qué se comprueba; la compatibilidad real se consulta en el dispositivo.
 */
object CatalogoPruebas {

    private const val REPETIR_LIMPIO =
        "Repite la prueba con la pantalla limpia y seca, sin guantes ni protector grueso."

    val pruebas: List<DefinicionPrueba> = listOf(
        // ---- Pantalla (D2) ----
        DefinicionPrueba(
            P.TACTIL, "Prueba táctil por zonas", "Panel táctil", C.PANTALLA,
            listOf(
                "La pantalla se dividirá en una cuadrícula.",
                "Desliza el dedo hasta marcar todas las zonas, incluidos los bordes.",
                "Usa el gesto o botón Atrás para abrir las opciones si alguna zona no responde."
            ),
            criterio = "Correcto si todas las zonas registran contacto. Falló si indicas que alguna no responde.",
            recomendacionFallo = "$REPETIR_LIMPIO Si las mismas zonas siguen sin responder, " +
                "el digitalizador puede estar dañado: consulta un servicio técnico.",
            implementada = true
        ),
        DefinicionPrueba(
            P.MULTITACTIL, "Multitáctil", "Panel táctil", C.PANTALLA,
            listOf(
                "Apoya varios dedos a la vez sobre la pantalla.",
                "Cada contacto se mostrará con un círculo numerado.",
                "La prueba termina al alcanzar el número de contactos requerido y levantar los dedos."
            ),
            criterio = "Correcto si se detectan simultáneamente los contactos que Android declara " +
                "(5 con soporte completo, 2 con soporte básico).",
            recomendacionFallo = "$REPETIR_LIMPIO Si no se alcanzan los contactos, el panel puede tener " +
                "zonas sin respuesta: ejecuta también la prueba táctil por zonas.",
            implementada = true
        ),
        DefinicionPrueba(
            P.COLORES, "Colores", "Pantalla", C.PANTALLA,
            listOf(
                "Se mostrarán colores sólidos a pantalla completa con el brillo al máximo.",
                "Toca la pantalla para pasar al siguiente color.",
                "Busca manchas, líneas, zonas más oscuras o tonos incorrectos."
            ),
            criterio = "Prueba visual: el resultado depende de tu respuesta tras ver todos los colores.",
            recomendacionFallo = "Las manchas o líneas persistentes suelen indicar daño en el panel. " +
                "Toma una foto del defecto y consulta un servicio técnico.",
            implementada = true
        ),
        DefinicionPrueba(
            P.PIXELES, "Píxeles", "Pantalla", C.PANTALLA,
            listOf(
                "Se mostrarán fondos negro, blanco y gris con el brillo al máximo.",
                "Recorre la pantalla con la vista buscando puntos que no cambien de color.",
                "Toca la pantalla para pasar al siguiente fondo."
            ),
            criterio = "Inspección guiada: el resultado depende de tu respuesta.",
            recomendacionFallo = "Un píxel atascado a veces se recupera; uno muerto no. " +
                "Si los puntos son varios o crecen, consulta un servicio técnico.",
            implementada = true
        ),

        // ---- Movimiento y sensores (D3) ----
        DefinicionPrueba(
            P.ACELEROMETRO, "Acelerómetro", "Sensor de aceleración", C.MOVIMIENTO,
            listOf(
                "Deja el teléfono quieto sobre una superficie unos segundos.",
                "Después gíralo lentamente para que cada lado apunte hacia arriba y hacia abajo."
            ),
            criterio = "Correcto si en reposo mide la gravedad (9,81 ± 1 m/s²) y cada eje recorre al menos 14 m/s².",
            recomendacionFallo = "Reinicia el teléfono y repite la prueba. Si la gravedad medida sigue " +
                "fuera de rango, el sensor puede estar descalibrado o dañado.",
            implementada = true
        ),
        DefinicionPrueba(
            P.GIROSCOPIO, "Giroscopio", "Sensor de rotación", C.MOVIMIENTO,
            listOf(
                "Deja el teléfono quieto unos segundos.",
                "Después gíralo con decisión alrededor de cada uno de sus tres ejes."
            ),
            criterio = "Correcto si en reposo marca menos de 0,1 rad/s y cada eje supera 1,5 rad/s al girar.",
            recomendacionFallo = "Reinicia el teléfono y repite la prueba. Una deriva alta en reposo " +
                "o ejes sin respuesta indican un problema del sensor.",
            implementada = true
        ),
        DefinicionPrueba(
            P.PROXIMIDAD, "Proximidad", "Sensor de proximidad", C.MOVIMIENTO,
            listOf(
                "Cubre con la mano la parte superior de la pantalla, cerca del auricular.",
                "Retira la mano después."
            ),
            criterio = "Correcto si el sensor informa «cerca» al cubrirlo y «lejos» al descubrirlo.",
            recomendacionFallo = "Limpia la zona del auricular y retira protectores que la cubran. " +
                "Si sigue sin cambiar, la pantalla podría no apagarse en llamadas.",
            implementada = true
        ),
        DefinicionPrueba(
            P.LUZ, "Luz ambiental", "Sensor de luz", C.MOVIMIENTO,
            listOf(
                "Apunta la parte superior del teléfono hacia una fuente de luz.",
                "Después cubre esa zona con la mano."
            ),
            criterio = "Correcto si se mide al menos 10 lx y al cubrir baja al 30 % o menos del máximo.",
            recomendacionFallo = "Limpia la zona superior y retira protectores. Si no cambia, " +
                "el brillo automático no funcionará correctamente.",
            implementada = true
        ),
        DefinicionPrueba(
            P.MAGNETOMETRO, "Magnetómetro", "Brújula", C.MOVIMIENTO,
            listOf(
                "Aléjate de imanes, altavoces y objetos metálicos grandes.",
                "Gira el teléfono lentamente en todas las direcciones."
            ),
            criterio = "Correcto si cada eje varía al menos 20 μT al girar y el campo medio está entre 20 y 80 μT.",
            recomendacionFallo = "Calibra la brújula moviendo el teléfono en forma de ocho lejos de metales " +
                "y repite. Si no varía, el sensor puede estar dañado.",
            implementada = true
        ),

        // ---- Cámara (D4) ----
        camara(P.CAMARA_TRASERA, "Cámara trasera", "Cámara principal"),
        camara(P.CAMARA_FRONTAL, "Cámara frontal", "Cámara frontal"),
        DefinicionPrueba(
            P.FLASH, "Flash", "Linterna / flash", C.CAMARA,
            listOf(
                "Mira la parte trasera del teléfono, junto a la cámara.",
                "El flash parpadeará un número aleatorio de veces.",
                "Indica cuántos destellos viste."
            ),
            criterio = "Correcto si indicas el mismo número de destellos que se emitieron.",
            recomendacionFallo = "Cierra otras apps que usen la cámara y repite. Si el flash no enciende, " +
                "el LED o su circuito pueden estar dañados.",
            implementada = true
        ),

        // ---- Audio (D4) ----
        DefinicionPrueba(
            P.ALTAVOZ, "Altavoz", "Altavoz principal", C.AUDIO,
            listOf(
                "Desconecta auriculares.",
                "El altavoz emitirá un número aleatorio de pitidos (el volumen multimedia se ajusta durante la prueba).",
                "Indica cuántos pitidos escuchaste."
            ),
            criterio = "Correcto si indicas el mismo número de pitidos emitidos por el altavoz integrado.",
            recomendacionFallo = "Limpia la rejilla del altavoz y repite. Si no se oye nada con el volumen " +
                "alto, el altavoz puede estar dañado.",
            implementada = true
        ),
        DefinicionPrueba(
            P.MICROFONO, "Micrófono", "Micrófono principal", C.AUDIO,
            listOf(
                "Permanece en silencio durante el primer segundo y medio.",
                "Después habla en voz alta o aplaude cerca del teléfono.",
                "Al terminar podrás reproducir la grabación. No se guarda en el dispositivo."
            ),
            criterio = "Correcto si el nivel sube al menos 15 dB sobre el silencio inicial y supera −45 dBFS.",
            recomendacionFallo = "Limpia el orificio del micrófono y cierra apps que lo usen. Si el nivel " +
                "no cambia al hablar, el micrófono puede estar dañado.",
            implementada = true,
            permisos = listOf(PermisoPrueba("android.permission.RECORD_AUDIO", descripcion = "Micrófono"))
        ),

        // ---- Vibración (D4) ----
        DefinicionPrueba(
            P.VIBRACION, "Vibración", "Motor de vibración", C.VIBRACION,
            listOf(
                "Sostén el teléfono en la mano.",
                "Vibrará un número aleatorio de veces.",
                "Indica cuántas vibraciones sentiste."
            ),
            criterio = "Correcto si indicas el mismo número de pulsos de vibración emitidos.",
            recomendacionFallo = "Comprueba que la vibración no esté desactivada en los ajustes de " +
                "accesibilidad. Si no vibra, el motor puede estar dañado.",
            implementada = true
        ),

        // ---- Conectividad (D5) ----
        DefinicionPrueba(
            P.WIFI, "Wi-Fi", "Radio Wi-Fi", C.CONECTIVIDAD,
            listOf(
                "Activa el Wi-Fi si está apagado.",
                "Conéctate a una red Wi-Fi conocida.",
                "La prueba termina sola al detectar la conexión."
            ),
            criterio = "Correcto si el teléfono se conecta a una red Wi-Fi. La intensidad y el acceso a " +
                "internet se registran como evidencia.",
            recomendacionFallo = "Reinicia el router y el teléfono y olvida y vuelve a añadir la red. Si no " +
                "detecta ninguna red, la antena Wi-Fi puede estar dañada.",
            implementada = true
        ),
        DefinicionPrueba(
            P.BLUETOOTH, "Bluetooth", "Radio Bluetooth", C.CONECTIVIDAD,
            listOf(
                "Activa el Bluetooth si está apagado.",
                "Ten cerca algún dispositivo Bluetooth encendido (auriculares, reloj, otro teléfono).",
                "Se buscarán dispositivos Bluetooth de bajo consumo durante unos segundos."
            ),
            criterio = "Correcto si el escaneo encuentra al menos un dispositivo cercano.",
            recomendacionFallo = "Reinicia el teléfono y repite cerca de otro dispositivo Bluetooth. Si nunca " +
                "encuentra ninguno, la antena puede estar dañada.",
            implementada = true,
            permisos = listOf(
                PermisoPrueba("android.permission.BLUETOOTH_SCAN", apiMinima = 31, descripcion = "Dispositivos cercanos"),
                PermisoPrueba(
                    "android.permission.ACCESS_FINE_LOCATION", apiMaxima = 30,
                    descripcion = "Ubicación (necesaria para buscar Bluetooth en Android 11 o anterior)"
                )
            )
        ),
        DefinicionPrueba(
            P.GPS, "GPS", "Receptor GNSS", C.CONECTIVIDAD,
            listOf(
                "Activa la ubicación del teléfono.",
                "Sal al exterior o acércate a una ventana con vista al cielo.",
                "Espera a que el receptor vea satélites y calcule una posición (puede tardar un minuto)."
            ),
            criterio = "Correcto si el proveedor GPS calcula una posición. Los satélites vistos se registran como evidencia.",
            recomendacionFallo = "Repite al aire libre lejos de edificios altos. Si nunca ve satélites, " +
                "la antena GNSS puede estar dañada.",
            implementada = true,
            permisos = listOf(
                PermisoPrueba("android.permission.ACCESS_FINE_LOCATION", descripcion = "Ubicación precisa"),
                PermisoPrueba("android.permission.ACCESS_COARSE_LOCATION", descripcion = "Ubicación aproximada")
            )
        ),

        // ---- Otros componentes (D5) ----
        DefinicionPrueba(
            P.BATERIA, "Batería", "Batería y carga", C.OTROS,
            listOf(
                "Se leerá el estado que informa el controlador de la batería.",
                "Después conecta el cargador para comprobar que se detecta la carga."
            ),
            criterio = "Correcto si la salud informada es «buena», la temperatura está entre 0 y 45 °C, " +
                "el voltaje entre 3,0 y 4,6 V y se detecta la carga al conectar el cargador.",
            recomendacionFallo = "Prueba con otro cable y cargador. Si la salud es mala o no detecta la carga, " +
                "revisa el puerto o la batería en un servicio técnico.",
            implementada = true
        ),
        DefinicionPrueba(
            P.ALMACENAMIENTO, "Almacenamiento", "Memoria interna", C.OTROS,
            listOf(
                "Se escribirá un archivo temporal de 32 MB, se leerá y se comprobará su integridad.",
                "El archivo se borra al terminar."
            ),
            criterio = "Correcto si los datos leídos coinciden con los escritos (SHA-256). " +
                "Las velocidades se registran como evidencia.",
            recomendacionFallo = "Libera espacio y repite. Si la integridad vuelve a fallar, " +
                "la memoria puede estar dañada: haz una copia de seguridad.",
            implementada = true
        ),
        DefinicionPrueba(
            P.RAM, "Memoria RAM", "RAM", C.OTROS,
            listOf(
                "Se reservará un bloque de memoria, se escribirá un patrón y se verificará.",
                "Solo se comprueba la memoria que Android asigna a la app, no toda la RAM."
            ),
            criterio = "Correcto si el patrón leído coincide con el escrito en todo el bloque reservado.",
            recomendacionFallo = "Reinicia el teléfono y repite. Errores de verificación repetidos pueden " +
                "indicar un fallo de memoria: consulta un servicio técnico.",
            implementada = true
        ),
        DefinicionPrueba(
            P.BOTONES, "Botones de volumen", "Botones físicos", C.OTROS,
            listOf(
                "Pulsa el botón de subir volumen.",
                "Pulsa el botón de bajar volumen.",
                "Android no permite a las apps comprobar los botones de encendido ni de inicio."
            ),
            criterio = "Correcto si se reciben las pulsaciones de ambos botones de volumen.",
            recomendacionFallo = "Retira fundas que presionen los botones y repite. Si uno no responde, " +
                "el pulsador puede estar dañado.",
            implementada = true
        )
    )

    private fun camara(id: P, nombre: String, componente: String) = DefinicionPrueba(
        id, nombre, componente, C.CAMARA,
        listOf(
            "Se abrirá la vista previa de la cámara.",
            "Apunta a un objeto bien iluminado y toma una foto de prueba.",
            "Revisa la foto: no se guarda en la galería."
        ),
        criterio = "Correcto si la cámara entrega vista previa, captura una foto y confirmas que se ve nítida.",
        recomendacionFallo = "Limpia la lente y cierra otras apps de cámara. Si la imagen sale negra, " +
            "borrosa o con manchas, la cámara puede estar dañada.",
        implementada = true,
        permisos = listOf(PermisoPrueba("android.permission.CAMERA", descripcion = "Cámara"))
    )

    private val porId = pruebas.associateBy { it.id }

    fun definicion(id: P): DefinicionPrueba = porId.getValue(id)

    /** Recomendación coherente con el resultado: nunca sugiere que algo funciona sin haberlo probado. */
    fun recomendacion(id: P, resultado: ResultadoPrueba): String = when (resultado) {
        ResultadoPrueba.CORRECTO -> "No se requiere ninguna acción."
        ResultadoPrueba.FALLO -> definicion(id).recomendacionFallo
        ResultadoPrueba.INDETERMINADO ->
            "No hubo evidencia suficiente para decidir. Repite la prueba siguiendo las instrucciones."
        ResultadoPrueba.NO_COMPATIBLE -> "Android no da acceso a este componente en este dispositivo."
        ResultadoPrueba.NO_EJECUTADO -> "La prueba no se completó; ejecútala para obtener un resultado."
    }

    /** Resultado de una prueba cancelada u omitida: nunca cuenta como aprobada. */
    fun noEjecutada(id: P, motivo: String) = ResultadoEvaluado(
        ResultadoPrueba.NO_EJECUTADO, listOf(motivo), recomendacion(id, ResultadoPrueba.NO_EJECUTADO)
    )
}
