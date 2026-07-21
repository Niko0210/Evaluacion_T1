package edu.pe.cibertec.taller.bdd;

import static org.mockito.ArgumentMatchers.any;

import edu.pe.cibertec.taller.excepcion.HorarioNoPermitidoException;
import edu.pe.cibertec.taller.excepcion.HorarioOcupadoException;
import edu.pe.cibertec.taller.modelo.*;
import edu.pe.cibertec.taller.repositorio.RepositorioCitas;
import edu.pe.cibertec.taller.repositorio.RepositorioMecanicos;
import edu.pe.cibertec.taller.servicio.impl.ServicioCitasImpl;
import edu.pe.cibertec.taller.util.ProveedorFechaHora;
import edu.pe.cibertec.taller.util.ServicioNotificaciones;
import io.cucumber.java.Before;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GestionCitasSteps {

	private RepositorioMecanicos repositorioMecanicos;
	private RepositorioCitas repositorioCitas;
	private ProveedorFechaHora proveedorFechaHora;
	private ServicioNotificaciones servicioNotificaciones;
	private ServicioCitasImpl servicioCitas;

	private Cita cita;
	private ResultadoCancelacion resultadoCancelacion;
	private Exception excepcion;
	private Mecanico mecanico;

	@Before
	public void inicializar() {
		repositorioMecanicos = mock(RepositorioMecanicos.class);
		repositorioCitas = mock(RepositorioCitas.class);
		proveedorFechaHora = mock(ProveedorFechaHora.class);
		servicioNotificaciones = mock(ServicioNotificaciones.class);
		servicioCitas = new ServicioCitasImpl(repositorioMecanicos, repositorioCitas,
				proveedorFechaHora, servicioNotificaciones);
	}

	// TODO: implementar aqui los pasos de los escenarios
	@Given("un mecanico disponible con especialidad {string}")
	public void unMecanicoDisponibleConEspecialidad(String especialidad) {
		mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.valueOf(especialidad));
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026,9,15,8,0));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(Collections.emptyList());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
	}
	@When("agendo una cita para el vehiculo {string} en la fecha {string}")
	public void agendoUnaCitaParaElVehiculoEnLaFecha(String placa,String fecha){
		try{
			cita = servicioCitas.agendarCita(1L, placa, mecanico.getEspecialidad(), LocalDateTime.parse(fecha));
		}catch(Exception e){
			excepcion=e;
		}
	}
	@Then("la cita queda en estado PROGRAMADA")
	public void citaQuedaEnEstadoProgramada(){
		assertNotNull(cita);
		assertEquals(EstadoCita.PROGRAMADA,cita.getEstado());
	}
	@And("se notifica el agendamiento")
	public void notificaElAgendamiento(){
		verify(servicioNotificaciones).notificarCitaAgendada(any(Cita.class));
	}

	@Given("un mecanico disponible con especialidad {string}")
	public void mecanicoDisponibleConEspecialidad(String especialidad) {
		mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.valueOf(especialidad));
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 15, 8, 0));
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(Collections.emptyList());
		when(repositorioCitas.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
	}
	@When("agendo una cita para el vehiculo {string} en la fecha {string}")
	public void agendoUnaCitaParaElVehiculoEnFecha(String placa, String fecha) {
		try {
			cita = servicioCitas.agendarCita(1L, placa, mecanico.getEspecialidad(), LocalDateTime.parse(fecha));
		} catch (Exception e) {
			excepcion = e;
		}
	}
	@Then("se lanza la excepcion HorarioNoPermitidoException")
	public void lanzaLaExcepcionHorarioNoPermitidoException() {
		assertNotNull(excepcion);
		assertTrue(excepcion instanceof HorarioNoPermitidoException);
	}
	@And("la cita no se guarda")
	public void citaNoSeGuarda() {
		verify(repositorioCitas, never()).save(any(Cita.class));
	}

	@Given("una cita programada para el {string}")
	public void citaProgramadaParaEl(String fecha) {
		cita = new Cita();
		cita.setId(1L);
		cita.setPlacaVehiculo("FLO-486");
		cita.setTipoServicio(TipoServicio.CAMBIO_ACEITE);
		cita.setFechaHoraInicio(LocalDateTime.parse(fecha));
		cita.setEstado(EstadoCita.PROGRAMADA);
		when(repositorioCitas.findById(1L)).thenReturn(Optional.of(cita));
	}
	@When("cancelo la cita")
	public void canceloLaCita() {
		try {
			resultadoCancelacion = servicioCitas.cancelarCita(1L);
		} catch (Exception e) {
			excepcion = e;
		}
	}
	@Then("la cita queda CANCELADA")
	public void citaQuedaCancelada() {
		assertNotNull(resultadoCancelacion);
		assertEquals(EstadoCita.CANCELADA, cita.getEstado());
	}
	@And("se aplica una penalidad de 50.00")
	public void aplicaUnaPenalidad_de_50() {
		assertEquals(50.0, resultadoCancelacion.getMontoPenalidad());
		verify(servicioNotificaciones).notificarCitaCancelada(cita);
	}

	@Given("un mecanico con una cita ya programada en {string}")
	public void mecanicoConUnaCitaYaProgramada(String fecha) {
		mecanico = new Mecanico(1L, "Nicols Flores", TipoServicio.REPARACION_MOTOR);
		when(repositorioMecanicos.findById(1L)).thenReturn(Optional.of(mecanico));
		when(proveedorFechaHora.ahora()).thenReturn(LocalDateTime.of(2026, 9, 15, 8, 0));
		Cita citaExistente = new Cita();
		citaExistente.setFechaHoraInicio(LocalDateTime.parse(fecha));
		citaExistente.setDuracionHoras(1);
		citaExistente.setEstado(EstadoCita.PROGRAMADA);
		when(repositorioCitas.findByMecanicoIdAndEstado(1L, EstadoCita.PROGRAMADA)).thenReturn(Collections.singletonList(citaExistente));
	}
	@When("intento agendar otra cita en el mismo horario")
	public void intentoAgendarOtraCitaEnElMismoHorario() {
		try {
			cita = servicioCitas.agendarCita(1L, "FLO-486", TipoServicio.REPARACION_MOTOR, LocalDateTime.of(2026, 9, 16, 10, 0));
		} catch (Exception e) {
			excepcion = e;
		}
	}
	@Then("se lanza la excepcion HorarioOcupadoException")
	public void lanzaLaExcepcionHorarioOcupadoException() {
		assertNotNull(excepcion);
		assertTrue(excepcion instanceof HorarioOcupadoException);
	}
	@And("la nueva cita no se guarda")
	public void nuevaCitaNoSeGuarda() {
		verify(repositorioCitas, never()).save(any(Cita.class));
	}
}
