from datetime import date
from typing import Literal
from pydantic import BaseModel, Field, model_validator


class Experience(BaseModel):
    id: str = Field(min_length=1)
    title: str
    domain: str
    role: str
    startDate: str | None = None
    endDate: str | None = None
    technologies: list[str] = Field(default_factory=list)
    description: str


class Employee(BaseModel):
    id: str
    name: str
    title: str
    department: str
    skills: list[str] = Field(default_factory=list)
    availability: Literal["AVAILABLE", "ASSIGNED", "UNKNOWN"]
    availableFrom: str | None = None
    allocation: int = Field(default=0, ge=0, le=100)
    experiences: list[Experience] = Field(default_factory=list)
    assignment: dict | None = None


class Requirement(BaseModel):
    id: str
    text: str = Field(min_length=1, max_length=1000)
    kind: Literal["MUST", "PREFERRED"] = "MUST"


class StaffingRequest(BaseModel):
    id: str = "draft"
    title: str = "추천 요청"
    domain: str
    description: str
    skills: list[str] = Field(default_factory=list)
    requirements: list[Requirement] = Field(default_factory=list)
    startDate: str | None = None
    endDate: str | None = None
    createdAt: str | None = None

    @model_validator(mode="after")
    def validate_dates(self):
        if self.startDate:
            date.fromisoformat(self.startDate)
        if self.endDate:
            date.fromisoformat(self.endDate)
        if self.startDate and self.endDate and self.endDate < self.startDate:
            raise ValueError("endDate precedes startDate")
        return self


class ParseRequest(BaseModel):
    domain: str = Field(default="", max_length=200)
    description: str = Field(min_length=1, max_length=10000)
    skills: list[str] = Field(default_factory=list, max_length=40)


class AnalysisPayload(BaseModel):
    request: StaffingRequest
    employees: list[Employee] = Field(max_length=10000)
