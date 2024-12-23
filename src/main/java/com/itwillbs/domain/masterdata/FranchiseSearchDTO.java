package com.itwillbs.domain.masterdata;

import java.time.LocalDate;

import lombok.Data;

@Data
public class FranchiseSearchDTO {
	private String franchiseName;
	private String ownerName;
	private String businessNumber;
	private LocalDate contractDateFrom;
	private LocalDate contractDateTo;
	private Boolean includeUnused = false;
}
