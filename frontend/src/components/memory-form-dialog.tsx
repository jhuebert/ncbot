import { useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button, Input, Textarea, Label } from "./ui/base";

const memorySchema = z.object({
  key: z.string().min(1, "Key is required").max(255, "Key is too long"),
  value: z.string().min(1, "Value is required").max(10_000, "Value is too long"),
});

export type MemoryFormValues = z.infer<typeof memorySchema>;

interface MemoryFormDialogProps {
  open: boolean;
  title: string;
  defaultValues?: MemoryFormValues;
  loading?: boolean;
  onSubmit: (values: MemoryFormValues) => void;
  onCancel: () => void;
}

export function MemoryFormDialog({
  open,
  title,
  defaultValues,
  loading = false,
  onSubmit,
  onCancel,
}: MemoryFormDialogProps) {
  const cancelRef = useRef<HTMLButtonElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MemoryFormValues>({
    resolver: zodResolver(memorySchema),
    defaultValues: defaultValues ?? { key: "", value: "" },
  });

  useEffect(() => {
    if (open) {
      reset(defaultValues ?? { key: "", value: "" });
      cancelRef.current?.focus();
    }
  }, [open, defaultValues, reset]);

  useEffect(() => {
    if (!open) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onCancel();
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [open, onCancel]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-label={title}
    >
      <div
        className="mx-4 w-full max-w-lg rounded-lg border border-gray-800 bg-gray-900 p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-base font-semibold text-gray-100">{title}</h2>
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="mt-4 space-y-4"
          noValidate
        >
          <div>
            <Label htmlFor="mem-key" required>
              Key
            </Label>
            <Input
              id="mem-key"
              {...register("key")}
              error={errors.key?.message}
              placeholder="e.g. user.alice"
              className="mt-1"
            />
          </div>
          <div>
            <Label htmlFor="mem-value" required>
              Value
            </Label>
            <Textarea
              id="mem-value"
              {...register("value")}
              error={errors.value?.message}
              placeholder="The synthesized memory value…"
              rows={4}
              className="mt-1"
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button
              ref={cancelRef}
              type="button"
              variant="secondary"
              size="sm"
              onClick={onCancel}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button type="submit" size="sm" disabled={loading}>
              {loading ? "Saving…" : "Save"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}