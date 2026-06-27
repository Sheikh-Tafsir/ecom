import {z} from "zod";
import {useParams} from "react-router-dom";
import {useMutation, useQueryClient} from "@tanstack/react-query";
import {Controller, useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";

import {Axios} from "@/services/http/Axios";

import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "@/components/ui/card";
import {
    Select,
    SelectContent,
    SelectGroup,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import {Label} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {Button} from "@/components/ui/button";
import {ButtonLoading} from "@/components/common/ButtonLoading";
import {notify} from "@/components/common/notification";
import {TOAST_TYPE} from "@/utils/enums";
import {handleErrors} from "@/utils";

const ReviewSchema = z.object({
    rating: z.number().int().min(1).max(5),
    comment: z.string().optional(),
});

const ReviewCreate = () => {
    const {id} = useParams();
    const queryClient = useQueryClient();

    const {
        register,
        control,
        handleSubmit,
        reset,
        setError,
        formState: {errors, isSubmitting},
    } = useForm({
        resolver: zodResolver(ReviewSchema),
        defaultValues: {
            rating: 5,
            comment: "",
        },
    });

    const createReviewMutation = useMutation({
        mutationFn: (data) => Axios.post(`/products/${id}/review`, data),

        onSuccess: async () => {
            notify(TOAST_TYPE.SUCCESS, "Review added successfully.");

            reset();
            await queryClient.invalidateQueries({queryKey: ["reviews", id]});
        },

        onError: (error) => {
            console.error(error);
            handleErrors(error, setError);
        },
    });

    const saveReview = (data) => {
        createReviewMutation.mutate(data);
    };

    return (
        <Card className="w-[49%] h-fit">
            <form onSubmit={handleSubmit(saveReview)}>
                <CardHeader>
                    <CardTitle>Add Review</CardTitle>
                </CardHeader>

                <CardContent className="space-y-4 px-6">
                    <div className="space-y-2">
                        <Label className="flex">
                            Rating
                            <span className="text-red-600">*</span>
                        </Label>

                        <Controller
                            name="rating"
                            control={control}
                            render={({field}) => (
                                <Select
                                    value={String(field.value)}
                                    onValueChange={(value) =>
                                        field.onChange(Number(value))
                                    }
                                >
                                    <SelectTrigger>
                                        <SelectValue placeholder="Select Rating"/>
                                    </SelectTrigger>

                                    <SelectContent>
                                        <SelectGroup>
                                            {[1, 2, 3, 4, 5].map((num) => (
                                                <SelectItem
                                                    key={num}
                                                    value={String(num)}
                                                >
                                                    {num} Star
                                                    {num > 1 && "s"}
                                                </SelectItem>
                                            ))}
                                        </SelectGroup>
                                    </SelectContent>
                                </Select>
                            )}
                        />

                        <p className="validation-error">
                            {errors.rating?.message}
                        </p>
                    </div>

                    <div className="space-y-2">
                        <Label className="flex">
                            Review
                            <span className="text-red-600">*</span>
                        </Label>

                        <Textarea
                            {...register("comment")}
                            placeholder="Write your review..."
                        />

                        <p className="validation-error">
                            {errors.comment?.message}
                        </p>
                    </div>
                </CardContent>

                <CardFooter className="flex-col gap-2">
                    {isSubmitting ? (
                        <ButtonLoading/>
                    ) : (
                        <Button
                            type="submit"
                            className="w-full bg-blue-600"
                        >
                            Add Review
                        </Button>
                    )}
                </CardFooter>
            </form>
        </Card>
    );
};

export default ReviewCreate;