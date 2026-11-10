package com.example.mobile_project.shared.services;

import com.example.mobile_project.dashboard.DashboardResponse;
import com.example.mobile_project.login.LoginRequest;
import com.example.mobile_project.login.LoginResponse;
import com.example.mobile_project.notificaciones.NotificacionesPageResponse;
import com.example.mobile_project.ticket.EstadoTicket;
import com.example.mobile_project.ticket.FormOptionsResponse;
import com.example.mobile_project.ticket.TicketCreateResponse;
import com.example.mobile_project.ticket.TicketDetailResponse;
import com.example.mobile_project.ticket.TicketsPageResponse;
import com.example.mobile_project.usuarios.ApiMessage;
import com.example.mobile_project.usuarios.CatalogoItem;
import com.example.mobile_project.usuarios.UsuarioDetalleResponse;
import com.example.mobile_project.usuarios.UsuarioItem;
import com.example.mobile_project.usuarios.UsuarioRequest;
import com.example.mobile_project.usuarios.UsuariosListResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiService {

    // ===================== AUTH =====================
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("logout")
    Call<Void> logout(@Header("Authorization") String token);

    // ===================== TICKETS =====================
    @GET("api/tickets/my-tickets")
    Call<TicketsPageResponse> getMyTickets(
            @Header("Authorization") String token,
            @Query("page") int page,
            @Query("filtro") String filtro);

    @GET("api/tickets/options")
    Call<FormOptionsResponse> getFormOptions(@Header("Authorization") String token);

    @GET("api/estados-ticket")
    Call<List<EstadoTicket>> getEstadosTicket(@Header("Authorization") String token);

    @Multipart
    @POST("api/tickets")
    Call<TicketCreateResponse> createTicket(
            @Header("Authorization") String token,
            @Part("titulo") RequestBody titulo,
            @Part("descripcion") RequestBody descripcion,
            @Part("servicio_id") RequestBody servicioId,
            @Part("prioridad_id") RequestBody prioridadId,
            @Part("ubicacion") RequestBody ubicacion,
            @Part MultipartBody.Part archivo
    );

    @GET("api/tickets/{idOrCodigo}")
    Call<TicketDetailResponse> getTicketDetail(
            @Header("Authorization") String token,
            @Path("idOrCodigo") String idOrCodigo
    );

    @GET("api/tickets/{codigo}/mensajes")
    Call<TicketDetailResponse.MensajesResponse> getMensajes(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Query("despues_de") long despuesDe);

    @POST("api/tickets/{codigo}/mensajes")
    Call<ResponseBody> enviarMensaje(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Body Map<String, Object> body);

    @POST("api/tickets/{codigo}/estado")
    Call<ResponseBody> cambiarEstado(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Body Map<String, Object> body);

    @POST("api/tickets/{codigo}/asignar")
    Call<ResponseBody> asignarTecnico(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Body Map<String, Object> body);

    @GET("api/tecnicos")
    Call<List<TicketDetailResponse.Tecnico>> getTecnicos(@Header("Authorization") String token);

    // ===================== TICKETS: reabrir / encuesta / complejidad y plazo =====================
    @POST("api/tickets/{codigo}/reabrir")
    Call<ResponseBody> reabrirTicket(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Body Map<String, Object> body);          // {"motivo": "..."}

    @POST("api/tickets/{codigo}/encuesta")
    Call<ResponseBody> enviarEncuesta(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Body Map<String, Object> body);          // {"calificacion": 1..5, "comentario": "..."}

    @POST("api/tickets/{codigo}/gestion")
    Call<ResponseBody> actualizarGestion(
            @Header("Authorization") String token,
            @Path("codigo") String codigo,
            @Body Map<String, Object> body);          // {"complejidad": "BAJA|MEDIA|ALTA", "plazo_dias": 0..90}

    // ===================== DASHBOARD SLA (solo administrador) =====================
    @GET("api/dashboard/sla")
    Call<DashboardResponse> getDashboardSla(
            @Header("Authorization") String token,
            @Query("periodo") String periodo,          // semana | mes | anio
            @Query("servicio_id") Integer servicioId,  // null = todos
            @Query("prioridad_id") Integer prioridadId);

    @Streaming
    @GET("api/dashboard/sla/exportar")
    Call<ResponseBody> exportarDashboardSla(
            @Header("Authorization") String token,
            @Query("periodo") String periodo,
            @Query("servicio_id") Integer servicioId,
            @Query("prioridad_id") Integer prioridadId);

    // ===================== FCM / NOTIFICACIONES =====================
    @POST("api/fcm-token")
    Call<ResponseBody> guardarTokenFcm(
            @Header("Authorization") String token,
            @Body Map<String, Object> body);

    @GET("api/notificaciones")
    Call<NotificacionesPageResponse> getNotificaciones(
            @Header("Authorization") String token,
            @Query("page") int page);

    @GET("api/notificaciones/no-leidas")
    Call<Map<String, Integer>> getNotifNoLeidas(@Header("Authorization") String token);

    @POST("api/notificaciones/{id}/leida")
    Call<ResponseBody> marcarNotifLeida(
            @Header("Authorization") String token,
            @Path("id") long id);

    @POST("api/notificaciones/leer-todas")
    Call<ResponseBody> leerTodasNotif(@Header("Authorization") String token);

    // ===================== USUARIOS (CRUD, solo administrador) =====================
    @GET("api/usuarios")
    Call<UsuariosListResponse> getUsuarios(
            @Header("Authorization") String token,
            @Query("buscar") String buscar,
            @Query("rol_id") Integer rolId,
            @Query("activo") Integer activo,
            @Query("page") int page,
            @Query("per_page") int perPage);

    @GET("api/usuarios/{id}")
    Call<UsuarioItem> getUsuario(
            @Header("Authorization") String token,
            @Path("id") long id);

    @POST("api/usuarios")
    Call<UsuarioDetalleResponse> createUsuario(
            @Header("Authorization") String token,
            @Body UsuarioRequest body);

    @PUT("api/usuarios/{id}")
    Call<UsuarioDetalleResponse> updateUsuario(
            @Header("Authorization") String token,
            @Path("id") long id,
            @Body UsuarioRequest body);

    @PATCH("api/usuarios/{id}/estado")
    Call<UsuarioDetalleResponse> setEstadoUsuario(
            @Header("Authorization") String token,
            @Path("id") long id,
            @Body Map<String, Object> body);   // {"activo": true|false}

    @DELETE("api/usuarios/{id}")
    Call<ApiMessage> deleteUsuario(
            @Header("Authorization") String token,
            @Path("id") long id);

    @GET("api/catalogos/roles")
    Call<List<CatalogoItem>> getRoles(@Header("Authorization") String token);

    @GET("api/catalogos/areas")
    Call<List<CatalogoItem>> getAreas(@Header("Authorization") String token);
}