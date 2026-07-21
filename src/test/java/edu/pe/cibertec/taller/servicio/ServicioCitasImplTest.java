package edu.pe.cibertec.taller.servicio;

import edu.pe.cibertec.taller.excepcion.*;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.pe.cibertec.taller.modelo.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ServicioCitasImplTest {

	@Mock
	private RepositorioMecanicos repositorioMecanicos;

	@Mock
	private RepositorioCitas repositorioCitas;

	@Mock
	private ProveedorFechaHora proveedorFechaHora;

	@Mock
	private ServicioNotificaciones servicioNotificaciones;

	private ServicioCitasImpl servicioCitas;

	@BeforeEach
	void inicializar() {
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
		// TODO: crear aqui los datos comunes que necesiten los tests

	}

	@Test
	@DisplayName("Agendar una cita valida la guarda, notifica y la retorna en estado PROGRAMADA")
	void agendarCitaExitosa() {
		// Arrange
		String zafiroPlaca = "FLO-486";

		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 8, 0);
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 10, 0);

		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.CAMBIO_ACEITE);

		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(Collections.emptyList());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		Cita cita = servicioCitas.agendarCita(1L, zafiroPlaca, TipoServicio.CAMBIO_ACEITE, fecha);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
		assertEquals(1, cita.getDuracionHoras());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
		verify(servicioNotificaciones, times(1)).notificarCitaAgendada(any(Cita.class));
	}

	@Test
	@DisplayName("Agendar con un mecanico inexistente lanza MecanicoNoEncontradoException")
	void agendarConMecanicoInexistente() {
		// Arrange
		String zafiroPlaca = "FLO-486";

		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 10, 0);
		when(repositorioMecanicos.findById(99L)).thenReturn(Optional.empty());

		// Act y Assert
		assertThrows(MecanicoNoEncontradoException.class,
				() -> servicioCitas.agendarCita(99L, zafiroPlaca, TipoServicio.CAMBIO_ACEITE, fecha));
		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Agendar cuando la especialidad no coincide lanza EspecialidadIncorrectaException")
	void agendarConEspecialidadIncorrecta() {
		// Arrange
		String zafiroPlaca = "FLO-486";
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 10, 0);
		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.CAMBIO_ACEITE);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));

		// Act y Assert
		assertThrows(EspecialidadIncorrectaException.class,
				() -> servicioCitas.agendarCita(1L, zafiroPlaca, TipoServicio.REPARACION_MOTOR, fecha));
		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Un servicio pesado a las 15:00 se rechaza con HorarioNoPermitidoException")
	void agendarServicioPesadoEnLaTarde() {
		// Arrange
		String zafiro = "FLO-486";
		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.REPARACION_MOTOR);
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 8, 0);
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 15, 0);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));

		// Act y Assert
		assertThrows(HorarioNoPermitidoException.class,
				() -> servicioCitas.agendarCita(1L, zafiro, TipoServicio.REPARACION_MOTOR, fecha));
		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Un servicio pesado a las 09:00 se acepta y se guarda")
	void agendarServicioPesadoEnLaManana() {
		// Arrange
		String zafiro = "FLO-486";
		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.REPARACION_MOTOR);
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 8, 0);
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 9, 0);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(Collections.emptyList());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		Cita cita = servicioCitas.agendarCita(1L, zafiro, TipoServicio.REPARACION_MOTOR, fecha);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
		assertEquals(4, cita.getDuracionHoras());
		verify(repositorioCitas, times(1)).save(any(Cita.class));

	}

	@Test
	@DisplayName("Agendar en una fecha del pasado lanza FechaInvalidaException")
	void agendarConFechaEnElPasado() {
		// Arrange
		String zafiro = "FLO-486";
		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.REPARACION_MOTOR);
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 8, 0);
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 14, 10, 0);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(ahora);

		// Act y Assert
		assertThrows(FechaInvalidaException.class,
				() -> servicioCitas.agendarCita(1L, zafiro, TipoServicio.REPARACION_MOTOR, fecha));
		verify(repositorioCitas, never()).save(any());
	}

	@Test
	@DisplayName("Agendar sobre una cita ya programada se rechaza con HorarioOcupadoException")
	void agendarConSuperposicion() {
		// Arrange
		String zafiro = "FLO-486";
		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.REPARACION_MOTOR);
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 8, 0);
		LocalDateTime inicio = LocalDateTime.of(2026, 9, 16, 10, 0);
		Cita existente = new Cita();
		existente.setFechaHoraInicio(inicio);
		existente.setDuracionHoras(1);
		existente.setEstado(EstadoCita.PROGRAMADA);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(List.of(existente));

		// Act y Assert
		assertThrows(HorarioOcupadoException.class,
				() -> servicioCitas.agendarCita(1L, zafiro, TipoServicio.REPARACION_MOTOR, inicio));
		verify(repositorioCitas, never()).save(any());

	}

	@Test
	@DisplayName("Una cita que empieza justo cuando termina otra se acepta")
	void agendarCitaContigua() {
		// Arrange
		String zafiro = "FLO-486";
		Mecanico mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.REPARACION_MOTOR);
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 8, 0);
		Cita existente = new Cita();
		existente.setFechaHoraInicio(LocalDateTime.of(2026, 9, 16, 9, 0));
		existente.setDuracionHoras(1);
		existente.setEstado(EstadoCita.PROGRAMADA);
		LocalDateTime nueva = LocalDateTime.of(2026, 9, 16, 10, 0);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(List.of(existente));
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));

		// Act
		Cita cita = servicioCitas.agendarCita(1L, zafiro, TipoServicio.REPARACION_MOTOR, nueva);

		// Assert
		assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
		verify(repositorioCitas, times(1)).save(any(Cita.class));
	}

	@Test
	@DisplayName("Cancelar con 24 horas o mas de anticipacion no genera penalidad")
	void cancelarConAnticipacionSuficiente() {
		// Arrange
		String zafiro = "FLO-486";
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 15, 10, 0);
		Cita cita = new Cita();
		cita.setId(1L);
		cita.setPlacaVehiculo(zafiro);
		cita.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		cita.setFechaHoraInicio(LocalDateTime.of(2026, 9, 16, 10, 0));
		cita.setEstado(EstadoCita.PROGRAMADA);
		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioCitas.findById(1L)).thenReturn(Optional.of(cita));
		when(repositorioCitas.save(any(Cita.class))).thenReturn(cita);

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(1L);

		// Assert
		assertTrue(resultado.isExitoso());
		assertEquals(0.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("Cancelar con menos de 24 horas aplica una penalidad de 50.00")
	void cancelarConAvisoTardio() {
		// Arrange
		String zafiro = "FLO-486";
		LocalDateTime ahora = LocalDateTime.of(2026, 9, 16, 8, 0);
		Cita cita = new Cita();
		cita.setId(1L);
		cita.setPlacaVehiculo(zafiro);
		cita.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		cita.setFechaHoraInicio(LocalDateTime.of(2026, 9, 16, 10, 0));
		cita.setEstado(EstadoCita.PROGRAMADA);
		when(proveedorFechaHora.ahora()).thenReturn(ahora);
		when(repositorioCitas.findById(1L)).thenReturn(Optional.of(cita));
		when(repositorioCitas.save(any(Cita.class))).thenReturn(cita);

		// Act
		ResultadoCancelacion resultado = servicioCitas.cancelarCita(1L);

		// Assert
		assertTrue(resultado.isExitoso());
		assertEquals(50.0, resultado.getMontoPenalidad());
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
		verify(servicioNotificaciones, times(1)).notificarCitaCancelada(cita);
	}

	@Test
	@DisplayName("Cancelar una cita inexistente lanza CitaNoEncontradaException")
	void cancelarCitaInexistente() {
		// Arrange
		String zafiro = "FLO-486";
		when(repositorioCitas.findById(99L)).thenReturn(Optional.empty());

		// Act y Assert
		assertThrows(CitaNoEncontradaException.class, () -> servicioCitas.cancelarCita(99L));
		verify(repositorioCitas, never()).save(any());
		verify(servicioNotificaciones, never()).notificarCitaCancelada(any());
	}

	@Test
	@DisplayName("Cancelar una cita que ya fue cancelada lanza CitaNoCancelableException")
	void cancelarCitaYaCancelada() {
		// Arrange
		String zafiro = "FLO-486";
		Cita cita = new Cita();
		cita.setId(1L);
		cita.setPlacaVehiculo(zafiro);
		cita.setEstado(EstadoCita.CANCELADA);
		when(repositorioCitas.findById(1L)).thenReturn(Optional.of(cita));

		// Act y Assert
		assertThrows(CitaNoCancelableException.class, () -> servicioCitas.cancelarCita(1L));
		verify(repositorioCitas, never()).save(any());
		verify(servicioNotificaciones, never()).notificarCitaCancelada(any());
	}

	@Test
	@DisplayName("Buscar mecanico disponible retorna el primero sin citas superpuestas")
	void buscarMecanicoDisponibleRetornaPrimeroLibre() {
		// Arrange
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 10, 0);
		Mecanico mecanicoOcupado = new Mecanico(1L, "Nicols Flores", TipoServicio.CAMBIO_ACEITE);
		Mecanico mecanicoLibre = new Mecanico(2L, "Carlos Pérez", TipoServicio.CAMBIO_ACEITE);
		Cita citaExistente = new Cita(1L, mecanicoOcupado, "FLO-486",
				TipoServicio.CAMBIO_ACEITE, fecha, 1, EstadoCita.PROGRAMADA);
		when(repositorioMecanicos.findByEspecialidad(TipoServicio.CAMBIO_ACEITE))
				.thenReturn(Arrays.asList(mecanicoOcupado, mecanicoLibre));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA))
				.thenReturn(Collections.singletonList(citaExistente));
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA))
				.thenReturn(Collections.emptyList());

		// Act
		Mecanico elegido = servicioCitas.buscarMecanicoDisponible(TipoServicio.CAMBIO_ACEITE, fecha);

		// Assert
		assertEquals(mecanicoLibre.getId(), elegido.getId());
		assertEquals("Carlos Pérez", elegido.getNombre());
	}

	@Test
	@DisplayName("Buscar mecanico cuando ninguno esta libre lanza SinDisponibilidadException")
	void buscarMecanicoSinDisponibilidad() {
		// Arrange
		LocalDateTime fecha = LocalDateTime.of(2026, 9, 16, 10, 0);
		Mecanico mecanico1 = new Mecanico(1L, "Nicols Flores Uribe", TipoServicio.CAMBIO_ACEITE);
		Mecanico mecanico2 = new Mecanico(2L, "Carlos Pérez", TipoServicio.CAMBIO_ACEITE);
		Cita cita1 = new Cita(1L, mecanico1, "FLO-486",
				TipoServicio.CAMBIO_ACEITE, fecha, 1, EstadoCita.PROGRAMADA);
		Cita cita2 = new Cita(2L, mecanico2, "CAR-486",
				TipoServicio.CAMBIO_ACEITE, fecha, 1, EstadoCita.PROGRAMADA);
		when(repositorioMecanicos.findByEspecialidad(TipoServicio.CAMBIO_ACEITE))
				.thenReturn(Arrays.asList(mecanico1, mecanico2));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA))
				.thenReturn(Collections.singletonList(cita1));
		when(repositorioCitas.findByMecanicoIdAndEstado(2L, EstadoCita.PROGRAMADA))
				.thenReturn(Collections.singletonList(cita2));

		// Act y Assert
		assertThrows(SinDisponibilidadException.class,
				() -> servicioCitas.buscarMecanicoDisponible(TipoServicio.CAMBIO_ACEITE, fecha));
	}
}
