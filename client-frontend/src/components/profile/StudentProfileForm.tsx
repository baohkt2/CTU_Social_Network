'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { userService } from '@/services/userService';
import { categoryService } from '@/services/categoryService';
import {
    BatchInfo,
    CollegeInfo,
    FacultyInfo,
    GenderInfo,
    HierarchicalCategories,
    MajorInfo,
    StudentProfileUpdateRequest,
    User
} from '@/types';
import { useToast } from '@/hooks/useToast';
import { useAuth } from '@/contexts/AuthContext';
import UploadFile from '@/components/ui/UploadFile';

interface StudentProfileFormProps {
    user: User;
}

export default function StudentProfileForm({ user }: StudentProfileFormProps) {
    const router = useRouter();
    const { showToast } = useToast();
    const { updateUser } = useAuth();

    const [formData, setFormData] = useState<StudentProfileUpdateRequest | null>(null);
    const [dropdownData, setDropdownData] = useState({
        hierarchicalData: null as HierarchicalCategories | null,
        colleges: [] as CollegeInfo[],
        faculties: [] as FacultyInfo[],
        majors: [] as MajorInfo[],
        genders: [] as GenderInfo[],
        batches: [] as BatchInfo[]
    });

    const [filteredFaculties, setFilteredFaculties] = useState<FacultyInfo[]>([]);
    const [filteredMajors, setFilteredMajors] = useState<MajorInfo[]>([]);
    const [selectedCollege, setSelectedCollege] = useState<string>('');
    const [selectedFaculty, setSelectedFaculty] = useState<string>('');
    const [loading, setLoading] = useState(false);
    const [dataLoading, setDataLoading] = useState(true);

    useEffect(() => {
        const loadDropdownData = async () => {
            try {
                const hierarchicalData = await categoryService.getAllCategories();

                // Fix missing codes from backend
                if (hierarchicalData && hierarchicalData.colleges) {
                    hierarchicalData.colleges.forEach(c => {
                        if (!c.code) c.code = c.name;
                        if (c.faculties) {
                            c.faculties.forEach(f => {
                                if (!f.code) f.code = f.name;
                                if (f.majors) {
                                    f.majors.forEach(m => {
                                        if (!m.code) m.code = m.name;
                                    });
                                }
                            });
                        }
                    });
                }

                const colleges = hierarchicalData.colleges.map(college => ({
                    name: college.name,
                    code: college.code
                }));

                setDropdownData({
                    hierarchicalData,
                    colleges,
                    faculties: [],
                    majors: [],
                    genders: hierarchicalData.genders || [],
                    batches: hierarchicalData.batches || []
                });

                const collegeCode = user.collegeCode || '';
                const facultyCode = user.facultyCode || '';
                const majorCode = user.majorCode || '';

                console.log('User data for auto-select:', {
                    college: user.college,
                    collegeCode: user.collegeCode,
                    faculty: user.faculty, 
                    facultyCode: user.facultyCode,
                    major: user.major,
                    majorCode: user.majorCode,
                    batch: user.batch,
                    batchCode: user.batchCode,
                    gender: user.gender,
                    genderCode: user.genderCode
                });

                setSelectedCollege(collegeCode);
                setSelectedFaculty(facultyCode);

                if (collegeCode) {
                    const selectedCollegeData = hierarchicalData.colleges.find(c => c.code === collegeCode);
                    if (selectedCollegeData) {
                        const facultiesInCollege = selectedCollegeData.faculties.map(faculty => ({
                            name: faculty.name,
                            code: faculty.code,
                            college: { name: selectedCollegeData.name, code: collegeCode }
                        }));
                        setFilteredFaculties(facultiesInCollege);

                        if (facultyCode) {
                            const selectedFacultyData = selectedCollegeData.faculties.find(f => f.code === facultyCode);
                            if (selectedFacultyData) {
                                const majorsInFaculty = selectedFacultyData.majors.map(major => ({
                                    name: major.name,
                                    code: major.code,
                                    faculty: {
                                        name: selectedFacultyData.name,
                                        code: facultyCode,
                                        college: { name: selectedCollegeData.name, code: collegeCode }
                                    }
                                }));
                                setFilteredMajors(majorsInFaculty);
                            }
                        }
                    }
                }

                if (formData === null) {
                    setFormData({
                        fullName: user.fullName || '',
                        bio: user.bio || '',
                        studentId: user.studentId || '',
                        majorCode: majorCode,
                        batchYear: user.batch || '',
                        genderCode: user.genderCode || '',
                        avatarUrl: user.avatarUrl || '',
                        backgroundUrl: user.backgroundUrl || '',
                        collegeCode: collegeCode,
                        facultyCode: facultyCode
                    });
                }
            } catch (error) {
                console.error('Error loading dropdown data:', error);
                showToast('Không thể tải dữ liệu danh mục', 'error');
            } finally {
                setDataLoading(false);
            }
        };

        loadDropdownData();
    }, [user]);

    const handleCollegeChange = (collegeCode: string) => {
        setSelectedCollege(collegeCode);
        setSelectedFaculty('');
        setFormData(prev => prev ? { ...prev, collegeCode, facultyCode: '', majorCode: '' } : null);

        if (collegeCode && dropdownData.hierarchicalData) {
            const selectedCollegeData = dropdownData.hierarchicalData.colleges.find(c => c.code === collegeCode);
            if (selectedCollegeData) {
                const facultiesInCollege = selectedCollegeData.faculties.map(faculty => ({
                    name: faculty.name,
                    code: faculty.code,
                    college: { name: selectedCollegeData.name, code: collegeCode }
                }));
                setFilteredFaculties(facultiesInCollege);
                setFilteredMajors([]);
            }
        } else {
            setFilteredFaculties([]);
            setFilteredMajors([]);
        }
    };

    const handleFacultyChange = (facultyCode: string) => {
        setSelectedFaculty(facultyCode);
        setFormData(prev => prev ? { ...prev, facultyCode, majorCode: '' } : null);

        if (facultyCode && dropdownData.hierarchicalData && selectedCollege) {
            const selectedCollegeData = dropdownData.hierarchicalData.colleges.find(c => c.code === selectedCollege);
            const selectedFacultyData = selectedCollegeData?.faculties.find(f => f.code === facultyCode);
            if (selectedFacultyData && selectedCollegeData) {
                const majorsInFaculty = selectedFacultyData.majors.map(major => ({
                    name: major.name,
                    code: major.code,
                    faculty: {
                        name: selectedFacultyData.name,
                        code: facultyCode,
                        college: { name: selectedCollegeData.name, code: selectedCollege }
                    }
                }));
                setFilteredMajors(majorsInFaculty);
            }
        } else {
            setFilteredMajors([]);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!formData?.fullName || !formData.studentId || !formData.majorCode || !formData.genderCode) {
            showToast('Vui lòng điền đầy đủ thông tin bắt buộc', 'error');
            return;
        }
        setLoading(true);
        try {
            const updatedUser = await userService.updateMyProfile(formData);
            updateUser(updatedUser);
            showToast('Cập nhật thông tin thành công!', 'success');
            router.push('/');
        } catch (error) {
            console.error('Error updating profile:', error);
            showToast('Có lỗi xảy ra khi cập nhật thông tin', 'error');
        } finally {
            setLoading(false);
        }
    };

    if (dataLoading || formData === null) {
        return (
            <div className="flex justify-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-6 text-gray-700">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Full Name */}
                <div>
                    <label className="block text-sm font-medium text-gray-900 mb-2">
                        Họ và tên <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={formData.fullName}
                        onChange={(e) => setFormData({...formData, fullName: e.target.value})}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    />
                </div>

                {/* Student ID */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Mã số sinh viên <span className="text-red-500">*</span>
                    </label>
                    <input
                        type="text"
                        value={formData.studentId}
                        onChange={(e) => setFormData({...formData, studentId: e.target.value})}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    />
                </div>

                {/* College */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Trường <span className="text-red-500">*</span>
                    </label>
                    <select
                        value={selectedCollege}
                        onChange={(e) => handleCollegeChange(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    >
                        <option value="">Chọn trường</option>
                        {dropdownData.colleges.map((college) => (
                            <option key={college.code} value={college.code}>
                                {college.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Faculty */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Khoa <span className="text-red-500">*</span>
                    </label>
                    <select
                        value={selectedFaculty}
                        onChange={(e) => handleFacultyChange(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    >
                        <option value="">Chọn khoa</option>
                        {filteredFaculties.map((faculty) => (
                            <option key={faculty.code} value={faculty.code}>
                                {faculty.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Major */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Ngành học <span className="text-red-500">*</span>
                    </label>
                    <select
                        value={formData.majorCode}
                        onChange={(e) => setFormData({...formData, majorCode: e.target.value})}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                        disabled={!selectedFaculty}
                    >
                        <option value="">Chọn ngành học</option>
                        {filteredMajors.map((major) => (
                            <option key={major.code} value={major.code}>
                                {major.name}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Batch Year */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Niên khóa <span className="text-red-500">*</span>
                    </label>
                    <select
                        value={formData.batchYear}
                        onChange={(e) => setFormData({...formData, batchYear: e.target.value})}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    >
                        <option value="">Chọn niên khóa</option>
                        {dropdownData.batches.map((batch) => (
                            <option key={batch.year} value={batch.year}>
                                {batch.year}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Gender */}
                <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                        Giới tính <span className="text-red-500">*</span>
                    </label>
                    <select
                        value={formData.genderCode}
                        onChange={(e) => setFormData({...formData, genderCode: e.target.value})}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        required
                    >
                        <option value="">Chọn giới tính</option>
                        {dropdownData.genders.map((gender) => (
                            <option key={gender.code} value={gender.code}>
                                {gender.name}
                            </option>
                        ))}
                    </select>
                </div>
            </div>

            {/* Bio */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Giới thiệu bản thân
                </label>
                <textarea
                    value={formData.bio}
                    onChange={(e) => setFormData({...formData, bio: e.target.value})}
                    rows={4}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Viết vài dòng giới thiệu về bản thân..."
                />
            </div>

            {/* Avatar Upload */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Ảnh đại diện
                </label>
                <UploadFile
                    currentUser={user}
                    currentImageUrl={formData.avatarUrl}
                    onImageUploaded={(url) => setFormData({...formData, avatarUrl: url})}
                    imageType="avatar"
                    aspectRatio="1/1"
                    maxSizeMB={2}
                />
            </div>

            {/* Background Upload */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Ảnh bìa
                </label>
                <UploadFile
                    currentUser={user}
                    currentImageUrl={formData.backgroundUrl}
                    onImageUploaded={(url) => setFormData({...formData, backgroundUrl: url})}
                    imageType="background"
                    aspectRatio="3/1"
                    maxSizeMB={5}
                />
            </div>

            {/* Submit Button */}
            <div className="flex justify-end space-x-4">
                <button
                    type="button"
                    onClick={() => router.back()}
                    className="px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    Hủy
                </button>
                <button
                    type="submit"
                    disabled={loading}
                    className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
                >
                    {loading ? 'Đang cập nhật...' : 'Cập nhật thông tin'}
                </button>
            </div>
        </form>
    );
}
