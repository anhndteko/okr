package org.burningokr.controller.okr;

import java.util.Collection;
import javax.validation.Valid;
import org.burningokr.annotation.RestApiController;
import org.burningokr.dto.okr.NoteTopicDraftDto;
import org.burningokr.dto.okr.OkrTopicDraftDto;
import org.burningokr.mapper.interfaces.DataMapper;
import org.burningokr.model.okr.NoteTopicDraft;
import org.burningokr.model.okr.okrTopicDraft.OkrTopicDraft;
import org.burningokr.model.okrUnits.OkrBranch;
import org.burningokr.model.users.User;
import org.burningokr.service.okr.OkrTopicDraftService;
import org.burningokr.service.okrUnit.OkrUnitServiceFactory;
import org.burningokr.service.security.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestApiController
public class TopicDraftController {
  private OkrTopicDraftService okrTopicDraftService;
  private DataMapper<OkrTopicDraft, OkrTopicDraftDto> okrTopicDraftMapper;
  private DataMapper<NoteTopicDraft, NoteTopicDraftDto> noteTopicDraftMapper;
  private AuthorizationService authorizationService;
  private final OkrUnitServiceFactory<OkrBranch> okrTopicOkrServiceFactory;

  /**
   * Initialize TopicDraftController
   *
   * @param okrTopicDraftService a {@link OkrTopicDraftService} object
   * @param okrTopicDraftMapper {@link DataMapper} object with {@link OkrTopicDraft} and {@link
   *     OkrTopicDraftDto}
   * @param noteTopicDraftMapper
   */
  @Autowired
  public TopicDraftController(
      OkrTopicDraftService okrTopicDraftService,
      DataMapper<OkrTopicDraft, OkrTopicDraftDto> okrTopicDraftMapper,
      DataMapper<NoteTopicDraft, NoteTopicDraftDto> noteTopicDraftMapper,
      OkrUnitServiceFactory<OkrBranch> okrTopicOkrServiceFactory,
      AuthorizationService authorizationService) {
    this.okrTopicDraftService = okrTopicDraftService;
    this.okrTopicDraftMapper = okrTopicDraftMapper;
    this.noteTopicDraftMapper = noteTopicDraftMapper;
    this.authorizationService = authorizationService;
    this.okrTopicOkrServiceFactory = okrTopicOkrServiceFactory;
  }

  /**
   * API Endpoint to get all TopicDrafts.
   *
   * @return a {@link ResponseEntity} ok with a {@link Collection} of TopicDrafts
   */
  @GetMapping("/topicDrafts/all")
  public ResponseEntity<Collection<OkrTopicDraftDto>> getAllCompanies() {
    Collection<OkrTopicDraft> topicDrafts = okrTopicDraftService.getAllTopicDrafts();
    return ResponseEntity.ok(okrTopicDraftMapper.mapEntitiesToDtos(topicDrafts));
  }

  /**
   * API Endpoint to get all comments/notes from a Topic Draft.
   *
   * @param topicDraftId a long value
   * @return a {@link ResponseEntity} ok with a {@link Collection} of NoteTopicDraftDtos
   */
  @GetMapping("/topicDrafts/{topicDraftId}/notes")
  public ResponseEntity<Collection<NoteTopicDraftDto>> getNotesForTopicDraft(
      @PathVariable long topicDraftId) {
    Collection<NoteTopicDraft> noteTopicDrafts =
        okrTopicDraftService.getAllNotesForTopicDraft(topicDraftId);
    return ResponseEntity.ok(noteTopicDraftMapper.mapEntitiesToDtos(noteTopicDrafts));
  }

  /**
   * API Endpoint to update/edit a Topic Draft.
   *
   * @param topicDraftId a long value
   * @param okrTopicDraftDto a {@link OkrTopicDraftDto} object
   * @return a {@link ResponseEntity} ok with a Topic Draft
   */
  @PutMapping("/topicDrafts/{topicDraftId}")
  @PreAuthorize(
      "@authorizationService.isAdmin() "
          + "|| @authorizationService.isTopicDraftInitiator(#topicDraftId)")
  public ResponseEntity updateTopicResultById(
      @PathVariable long topicDraftId, @Valid @RequestBody OkrTopicDraftDto okrTopicDraftDto) {
    OkrTopicDraft okrTopicDraft = okrTopicDraftMapper.mapDtoToEntity(okrTopicDraftDto);
    this.okrTopicDraftService.updateOkrTopicDraft(topicDraftId, okrTopicDraft);
    return ResponseEntity.ok().build();
  }

  /**
   * API Endpoint to update the status of a Topic Draft.
   *
   * @param topicDraftId a long value
   * @param okrTopicDraftDto a {@link OkrTopicDraftDto} object
   * @return a {@link ResponseEntity} ok with a Topic Draft
   */
  @PutMapping("/topicDrafts/status/{topicDraftId}")
  @PreAuthorize(
      "@authorizationService.isAdmin()"
          + "|| @authorizationService.isAuditor()"
          + "|| @authorizationService.isTopicDraftInitiator(#topicDraftId)")
  public ResponseEntity updateTopicResultStatusById(
      @PathVariable long topicDraftId, @Valid @RequestBody OkrTopicDraftDto okrTopicDraftDto) {
    OkrTopicDraft okrTopicDraft = okrTopicDraftMapper.mapDtoToEntity(okrTopicDraftDto);
    this.okrTopicDraftService.updateOkrTopicDraftStatus(topicDraftId, okrTopicDraft);
    return ResponseEntity.ok().build();
  }

  /**
   * API Endpoint to add a Note to a Topic Draft.
   *
   * @param topicDraftId a long value
   * @param noteTopicDraftDto a {@link NoteTopicDraftDto} object
   * @param user an {@link User} object
   * @return a {@link ResponseEntity} ok with a NoteTopicDraftDto
   */
  @PostMapping("/topicDrafts/{topicDraftId}/notes")
  public ResponseEntity<NoteTopicDraftDto> addNoteToTopicDraft(
      @PathVariable long topicDraftId,
      @Valid @RequestBody NoteTopicDraftDto noteTopicDraftDto,
      User user) {
    noteTopicDraftDto.setParentTopicDraftId(topicDraftId);
    NoteTopicDraft noteTopicDraft = noteTopicDraftMapper.mapDtoToEntity(noteTopicDraftDto);
    noteTopicDraft.setId(null);
    noteTopicDraft = this.okrTopicDraftService.createNote(topicDraftId, noteTopicDraft, user);
    return ResponseEntity.ok(noteTopicDraftMapper.mapEntityToDto(noteTopicDraft));
  }

  /**
   * API Endpoint to add a TopicDraft to an existing Okr Branch
   *
   * @param topicDraftDto a {@link OkrTopicDraftDto} object
   * @param user an {@link User} object
   * @return a {@link ResponseEntity} ok with the added topicdraft
   */
  @PostMapping("/topicDrafts/create")
  public ResponseEntity<OkrTopicDraftDto> createOkrTopicDraft(
      @RequestBody OkrTopicDraftDto topicDraftDto, User user) {
    OkrTopicDraft topicDraft = okrTopicDraftMapper.mapDtoToEntity(topicDraftDto);
    OkrTopicDraft newOkrTopicDraft = okrTopicDraftService.createTopicDraft(topicDraft, user);
    OkrTopicDraftDto newOkrTopicDraftDto = okrTopicDraftMapper.mapEntityToDto(newOkrTopicDraft);
    return ResponseEntity.ok(newOkrTopicDraftDto);
  }

  /**
   * API Endpoint to delete a Topic Draft.
   *
   * @param topicDraftId a long value
   * @param user a {@link User} object
   * @return a {@link ResponseEntity} ok with a Topic Draft
   */
  @DeleteMapping("/topicDraft/{topicDraftId}")
  @PreAuthorize(
      "@authorizationService.isAdmin() "
          + "|| @authorizationService.isTopicDraftInitiator(#topicDraftId)")
  public ResponseEntity deleteTopicDraftById(@PathVariable Long topicDraftId, User user) {
    okrTopicDraftService.deleteTopicDraftById(topicDraftId, user);
    return ResponseEntity.ok().build();
  }
}
