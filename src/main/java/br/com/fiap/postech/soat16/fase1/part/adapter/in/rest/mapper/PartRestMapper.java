package br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.mapper;

import java.util.UUID;

import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.request.PartRequestDto;
import br.com.fiap.postech.soat16.fase1.part.adapter.in.rest.dto.response.PartResponseDto;
import br.com.fiap.postech.soat16.fase1.part.application.command.AdjustPartStockCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.CreatePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.DeletePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.command.FindPartQuery;
import br.com.fiap.postech.soat16.fase1.part.application.command.UpdatePartCommand;
import br.com.fiap.postech.soat16.fase1.part.application.result.PartResult;

public final class PartRestMapper {

    private PartRestMapper() {
    }

    public static CreatePartCommand toCreateCommand(PartRequestDto request) {
        return new CreatePartCommand(
                request.name(),
                request.description(),
                request.unitPrice(),
                request.stockQuantity(),
                request.unit(),
                request.minimumStock(),
                request.partType());
    }

    public static UpdatePartCommand toUpdateCommand(UUID id, PartRequestDto request) {
        return new UpdatePartCommand(
                id,
                request.name(),
                request.description(),
                request.unitPrice(),
                request.stockQuantity(),
                request.unit(),
                request.minimumStock(),
                request.partType());
    }

    public static FindPartQuery toQuery(UUID id) {
        return new FindPartQuery(id);
    }

    public static AdjustPartStockCommand toStockCommand(UUID id, int adjustment) {
        return new AdjustPartStockCommand(id, adjustment);
    }

    public static DeletePartCommand toDeleteCommand(UUID id) {
        return new DeletePartCommand(id);
    }

    public static PartResponseDto toResponse(PartResult result) {
        return new PartResponseDto(
                result.id(),
                result.name(),
                result.description(),
                result.unitPrice(),
                result.stockQuantity(),
                result.unit(),
                result.minimumStock(),
                result.partType(),
                result.lowStock(),
                result.createdAt());
    }
}
