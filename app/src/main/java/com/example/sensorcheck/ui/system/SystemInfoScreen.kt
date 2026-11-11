package com.example.sensorcheck.ui.system

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorcheck.domain.format.FormatoSistema
import com.example.sensorcheck.domain.model.EstadoCifrado
import com.example.sensorcheck.domain.model.SystemInfo
import com.example.sensorcheck.domain.model.TiempoActividad
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.DatoInfo
import com.example.sensorcheck.ui.components.GrupoInfo
import com.example.sensorcheck.ui.components.SeccionExpandible
import com.example.sensorcheck.ui.components.TituloSeccion
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
fun SystemInfoScreen(
    viewModel: SystemViewModel = viewModel()
) {
    val info by viewModel.informacion.collectAsStateWithLifecycle()
    val tiempo by viewModel.tiempoActividad.collectAsStateWithLifecycle()

    // Refrescar al volver a la pantalla (idioma, zona horaria, ajustes)
    LifecycleResumeEffect(Unit) {
        viewModel.actualizar()
        onPauseOrDispose { }
    }

    val actual = info
    if (actual == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ContenidoSistema(actual, tiempo)
    }
}

@Composable
private fun ContenidoSistema(
    info: SystemInfo,
    tiempo: TiempoActividad?
) {
    val configuracion = LocalConfiguration.current
    val locale: Locale = configuracion.locales.get(0) ?: Locale.getDefault()
    var avanzadaExpandida by rememberSaveable { mutableStateOf(false) }

    val so = info.sistemaOperativo
    val seg = info.seguridad
    val kernel = info.kernel
    val reg = info.regional
    val comp = info.compilacion

    val mesesParche = FormatoSistema.mesesDesdeParche(seg.parcheSeguridad, LocalDate.now())

    ContenidoPantalla {

        item { TituloSeccion("Sistema operativo") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(Icons.Outlined.Android, "Sistema operativo", so.nombre),
                    DatoInfo(Icons.Outlined.Info, "Versión", so.version),
                    DatoInfo(Icons.Outlined.Code, "Nivel de API", so.nivelApi.toString()),
                    DatoInfo(Icons.Outlined.Tag, "Identificador de compilación", so.idCompilacion, copiable = true),
                    DatoInfo(Icons.Outlined.Numbers, "Número de compilación", so.numeroCompilacion, copiable = true)
                )
            )
        }

        item { TituloSeccion("Seguridad") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(
                        Icons.Outlined.Security,
                        "Parche de seguridad",
                        FormatoSistema.fechaParche(seg.parcheSeguridad, locale) ?: seg.parcheSeguridad
                    ),
                    DatoInfo(Icons.Outlined.History, "Antigüedad del parche", FormatoSistema.antiguedad(mesesParche)),
                    DatoInfo(Icons.Outlined.Build, "Tipo de compilación", FormatoSistema.tipoCompilacion(seg.tipoCompilacion)),
                    DatoInfo(
                        Icons.Outlined.BugReport,
                        "Compilación depurable",
                        FormatoSistema.siNo(FormatoSistema.compilacionDepurable(seg.tipoCompilacion))
                    ),
                    DatoInfo(Icons.Outlined.Key, "Firma del sistema", FormatoSistema.clavesFirma(seg.etiquetas)),
                    DatoInfo(Icons.Outlined.Usb, "Depuración USB", activada(seg.depuracionUsbActiva)),
                    DatoInfo(Icons.Outlined.DeveloperMode, "Opciones de desarrollador", activada(seg.opcionesDesarrolladorActivas)),
                    DatoInfo(Icons.Outlined.Lock, "Bloqueo de pantalla seguro", FormatoSistema.siNo(seg.bloqueoPantallaSeguro)),
                    DatoInfo(Icons.Outlined.EnhancedEncryption, "Cifrado del almacenamiento", textoCifrado(seg.cifrado))
                )
            )
        }
        item {
            Text(
                text = "El nivel de parche por sí solo no garantiza que el dispositivo sea seguro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item { TituloSeccion("Kernel y arquitectura") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(
                        Icons.Outlined.Terminal,
                        "Kernel Linux",
                        kernel.version ?: FormatoSistema.versionKernelDesdeProc(kernel.cadenaCompleta),
                        copiable = true
                    ),
                    DatoInfo(
                        Icons.Outlined.Fingerprint,
                        "Identificador del kernel",
                        FormatoSistema.identificadorKernel(kernel.cadenaCompleta),
                        copiable = true
                    ),
                    DatoInfo(Icons.Outlined.Memory, "Arquitectura del sistema", kernel.arquitectura),
                    DatoInfo(Icons.Outlined.Layers, "Arquitecturas compatibles", FormatoSistema.lista(kernel.abis)),
                    DatoInfo(Icons.Outlined.Layers, "ABI de 64 bits", FormatoSistema.lista(kernel.abis64)),
                    DatoInfo(Icons.Outlined.Layers, "ABI de 32 bits", FormatoSistema.lista(kernel.abis32))
                )
            )
        }

        item { TituloSeccion("Tiempo de actividad") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(
                        Icons.Outlined.Timer,
                        "Desde el último arranque",
                        tiempo?.let { FormatoSistema.tiempoActividad(it.desdeArranqueMs) }
                    ),
                    DatoInfo(
                        Icons.Outlined.RestartAlt,
                        "Último arranque",
                        tiempo?.let {
                            FormatoSistema.fechaHora(it.arranqueEpochMs, locale, ZoneId.systemDefault())
                        }
                    )
                )
            )
        }

        item { TituloSeccion("Idioma y región") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(
                        Icons.Outlined.Translate,
                        "Idioma",
                        conCodigo(FormatoSistema.capitalizar(reg.idioma, locale), reg.codigoIdioma)
                    ),
                    DatoInfo(Icons.Outlined.Public, "Región", conCodigo(reg.region, reg.codigoRegion)),
                    DatoInfo(Icons.Outlined.Schedule, "Zona horaria", reg.zonaHorariaId),
                    DatoInfo(
                        Icons.Outlined.AccessTime,
                        "Nombre y desfase horario",
                        listOfNotNull(
                            FormatoSistema.capitalizar(reg.zonaHorariaNombre, locale),
                            reg.desfaseUtcMs?.let { FormatoSistema.desfaseUtc(it) }
                        ).joinToString(" · ").ifBlank { null }
                    ),
                    DatoInfo(Icons.Outlined.Language, "Configuración regional", reg.etiquetaLocale),
                    DatoInfo(
                        Icons.AutoMirrored.Outlined.FormatListBulleted,
                        "Idiomas preferidos",
                        FormatoSistema.lista(reg.idiomasPreferidos.mapNotNull { FormatoSistema.capitalizar(it, locale) })
                    )
                )
            )
        }

        item {
            SeccionExpandible(
                titulo = "Información avanzada",
                expandida = avanzadaExpandida,
                onCambiar = { avanzadaExpandida = !avanzadaExpandida }
            ) {
                GrupoInfo(
                    listOf(
                        DatoInfo(Icons.Outlined.Fingerprint, "Huella de compilación", comp.huella, copiable = true),
                        DatoInfo(Icons.Outlined.Numbers, "Versión incremental", comp.incremental, copiable = true),
                        DatoInfo(Icons.Outlined.Sell, "Etiquetas", comp.etiquetas),
                        DatoInfo(Icons.Outlined.Badge, "Nombre en clave", comp.codename),
                        DatoInfo(Icons.Outlined.Event, "Fecha de compilación",
                            comp.fechaCompilacionMs?.let {
                                FormatoSistema.fechaHora(it, locale, ZoneId.systemDefault())
                            }
                        ),
                        DatoInfo(Icons.Outlined.Dns, "Host de compilación", comp.host),
                        DatoInfo(Icons.Outlined.Person, "Usuario de compilación", comp.usuario),
                        DatoInfo(Icons.Outlined.PowerSettingsNew, "Bootloader", comp.bootloader, copiable = true),
                        DatoInfo(Icons.Outlined.CellTower, "Banda base", comp.bandaBase, copiable = true),
                        DatoInfo(Icons.Outlined.Coffee, "Máquina virtual (ART)", comp.versionArt),
                        DatoInfo(Icons.Outlined.Inventory2, "Sistema base", comp.sistemaBase),
                        DatoInfo(
                            Icons.Outlined.Speed,
                            "Clase de rendimiento multimedia",
                            comp.claseRendimientoMultimedia?.let { if (it == 0) "No declarada" else it.toString() }
                        ),
                        DatoInfo(
                            Icons.AutoMirrored.Outlined.Article,
                            "Versión completa del kernel",
                            kernel.cadenaCompleta,
                            copiable = true
                        )
                    )
                )
            }
        }
    }
}

private fun activada(valor: Boolean?): String? =
    valor?.let { if (it) "Activada" else "Desactivada" }

private fun conCodigo(nombre: String?, codigo: String?): String? = when {
    nombre != null && !codigo.isNullOrBlank() -> "$nombre ($codigo)"
    else -> nombre ?: codigo
}

private fun textoCifrado(estado: EstadoCifrado): String? = when (estado) {
    EstadoCifrado.ACTIVO -> "Activo"
    EstadoCifrado.ACTIVO_POR_USUARIO -> "Activo (por archivo / por usuario)"
    EstadoCifrado.INACTIVO -> "Inactivo"
    EstadoCifrado.NO_COMPATIBLE -> "No compatible"
    EstadoCifrado.DESCONOCIDO -> null
}
