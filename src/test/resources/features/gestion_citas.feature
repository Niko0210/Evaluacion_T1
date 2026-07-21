Feature: Gestion de citas del taller mecanico

  # TODO: escribir aqui los 4 escenarios usando Given / When / Then / And:
  #
  # 1. Agendar un cambio de aceite de forma exitosa
  #    (la cita queda PROGRAMADA y se notifica el agendamiento)
  Scenario: Agendar un cambio de aceite de forma exitosa
    Given un mecanico disponible con especialidad "CAMBIO_ACEITE"
    When agendo una cita para el vehiculo "FLO-486" en la fecha "2026-09-16T10:00:00"
    Then la cita queda en estado PROGRAMADA
    And se notifica el agendamiento

  #
  # 2. Rechazar una reparacion de motor en la tarde
  #    (los servicios pesados solo se atienden entre las 08:00 y las 12:00)
  Scenario: Rechazar una reparacion de motor en la tarde
    Given un mecanico disponible con especialidad "REPARACION_MOTOR"
    When agendo una cita para el vehiculo "FLO-486" en la fecha "2026-09-16T15:00:00"
    Then se lanza la excepcion HorarioNoPermitidoException
    And la cita no se guarda

  #
  # 3. Cancelar con penalidad por aviso tardio
  #    (cancelar con menos de 24 horas aplica una penalidad de 50.00)
  Scenario: Cancelar con penalidad por aviso tardio
    Given una cita programada para el "2026-09-16T10:00:00"
    And la fecha actual es "2026-09-16T08:00:00"
    When cancelo la cita
    Then la cita queda CANCELADA
    And se aplica una penalidad de 50.00
  #
  # 4. Rechazar un agendamiento por horario ocupado
  #    (el mecanico ya tiene una cita programada que se superpone)
  Scenario: Rechazar un agendamiento por horario ocupado
    Given un mecanico con una cita ya programada en "2026-09-16T10:00:00"
    When intento agendar otra cita en el mismo horario
    Then se lanza la excepcion HorarioOcupadoException
    And la nueva cita no se guarda
