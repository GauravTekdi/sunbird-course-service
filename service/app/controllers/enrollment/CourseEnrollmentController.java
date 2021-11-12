package controllers.enrollment;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import controllers.enrollment.validator.CourseEnrollmentRequestValidator;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;
import org.sunbird.common.Common;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.CompletionStage;

public class CourseEnrollmentController extends BaseController {

  @Inject
  @Named("course-enrolment-actor")
  private ActorRef courseEnrolmentActor;

  public CompletionStage<Result> getEnrolledCourses(String uid, Http.Request httpRequest) {
    return handleRequest(courseEnrolmentActor, "listEnrol",
        httpRequest.body().asJson(),
        (req) -> {
          Request request = (Request) req;
          Map<String, String[]> queryParams = new HashMap<>(httpRequest.queryString());
          if(queryParams.containsKey("fields")) {
              Set<String> fields = new HashSet<>(Arrays.asList(queryParams.get("fields")[0].split(",")));
              fields.addAll(Arrays.asList(JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.LEAF_NODE_COUNT, JsonKey.APP_ICON));
              queryParams.put("fields", fields.toArray(new String[0]));
          }
          request
              .getContext()
              .put(JsonKey.URL_QUERY_STRING, getQueryString(queryParams));
          request
              .getContext()
              .put(JsonKey.BATCH_DETAILS, httpRequest.queryString().get(JsonKey.BATCH_DETAILS));
            if (queryParams.containsKey("cache")) {
                request.getContext().put("cache", Boolean.parseBoolean(queryParams.get("cache")[0]));
            } else
                request.getContext().put("cache", true);
          return null;
        },
        ProjectUtil.getLmsUserId(uid),
        JsonKey.USER_ID,
        getAllRequestHeaders((httpRequest)),
        false,
        httpRequest);
  }

  /*public CompletionStage<Result> getEnrolledCourse(Http.Request httpRequest) {
    return handleRequest(
        learnerStateActorRef,
        ActorOperations.GET_USER_COURSE.getValue(),
        httpRequest.body().asJson(),
        (req) -> {
          Request request = (Request) req;
          new CourseEnrollmentRequestValidator().validateEnrolledCourse(request);
          return null;
        },
        getAllRequestHeaders((httpRequest)),
        httpRequest);
  }*/

  public CompletionStage<Result> enrollCourse(Http.Request httpRequest) {
    return handleRequest(courseEnrolmentActor, "enrol",
        httpRequest.body().asJson(),
        (request) -> {
          Request req = (Request) request;
          Common.handleFixedBatchIdRequest(req);
          new CourseEnrollmentRequestValidator().validateEnrollCourse(req);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

  public CompletionStage<Result> unenrollCourse(Http.Request httpRequest) {
    return handleRequest(
            courseEnrolmentActor, "unenrol",
        httpRequest.body().asJson(),
        (request) -> {
          Request req = (Request) request;
          Common.handleFixedBatchIdRequest(req);
          new CourseEnrollmentRequestValidator().validateUnenrollCourse(req);
          return null;
        },
        getAllRequestHeaders(httpRequest),
        httpRequest);
  }

    public CompletionStage<Result> getParticipantsForFixedBatch(Http.Request httpRequest) {
        return handleRequest(courseEnrolmentActor, "getParticipantsForFixedBatch",
                httpRequest.body().asJson(),
                (request) -> {
                    Common.handleFixedBatchIdRequest((Request) request);
                    new CourseEnrollmentRequestValidator().validateCourseParticipant((Request) request);
                    return null;
                },
                getAllRequestHeaders(httpRequest),
                httpRequest);
    }

    public CompletionStage<Result> getUserEnrolledCourses(Http.Request httpRequest) {
        return handleRequest(
                courseEnrolmentActor, "listEnrol",
                httpRequest.body().asJson(),
                (req) -> {
                    Request request = (Request) req;
                    Map<String, String[]> queryParams = new HashMap<>(httpRequest.queryString());
                    if(queryParams.containsKey("fields")) {
                        Set<String> fields = new HashSet<>(Arrays.asList(queryParams.get("fields")[0].split(",")));
                        fields.addAll(Arrays.asList(JsonKey.NAME, JsonKey.DESCRIPTION, JsonKey.LEAF_NODE_COUNT, JsonKey.APP_ICON));
                        queryParams.put("fields", fields.toArray(new String[0]));
                    }
                    request
                            .getContext()
                            .put(JsonKey.URL_QUERY_STRING, getQueryString(queryParams));
                    request
                            .getContext()
                            .put(JsonKey.BATCH_DETAILS, httpRequest.queryString().get(JsonKey.BATCH_DETAILS));
                    new CourseEnrollmentRequestValidator().validateUserEnrolledCourse(request);
                    request.getContext().put(JsonKey.USER_ID, request.get(JsonKey.USER_ID));
                    return null;
                },
                getAllRequestHeaders((httpRequest)),
                httpRequest);
    }

    /**
     * Creates the attendance
     *
     * @param onlineProvider the online provider
     * @param httpRequest    the http request
     * @return the result
     * @throws JsonProcessingException the json processing exception
     */
    public CompletionStage<Result> createAttendance(String onlineProvider, Http.Request httpRequest) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return handleRequest(courseEnrolmentActor, "createAttendance",
                (Http.MimeTypes.FORM.equalsIgnoreCase(httpRequest.contentType().get())) ? mapper.readTree(JsonKey.START_CURLY_BRACE.concat("\"request\"").concat(JsonKey.COLON).concat(httpRequest.body().asFormUrlEncoded().get(JsonKey.BBB_WEBHOOK_RESP_BODY_EVENT)[0]).concat(JsonKey.END_CURLY_BRACE)) : httpRequest.body().asJson(),
                (request) -> {
                    new CourseEnrollmentRequestValidator().validateCreateAttendance(onlineProvider);
                    return null;
                },
                onlineProvider,
                JsonKey.ONLINE_PROVIDER,
                getAllRequestHeaders(httpRequest),
                true,
                httpRequest);
    }

    /**
     * Gets the attendance of the users enrolled in provided event and batch
     *
     * @param httpRequest the http request
     * @return the result
     */
    public CompletionStage<Result> getAttendance(Http.Request httpRequest) {
        return handleRequest(
                courseEnrolmentActor,
                "getAttendance",
                httpRequest.body().asJson(),
                (request) -> {
                    Request req = (Request) request;
                    new CourseEnrollmentRequestValidator().validateGetAttendance(req);
                    return null;
                },
                getAllRequestHeaders(httpRequest),
                httpRequest);
    }
}
