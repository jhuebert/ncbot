package org.huebert.ncbot.controller.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, int totalPages, int currentPage, int totalElements) {
}
